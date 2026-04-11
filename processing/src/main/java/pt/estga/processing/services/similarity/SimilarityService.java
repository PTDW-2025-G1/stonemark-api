package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.shared.utils.VectorUtils;
import java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Objects;
import java.util.HashSet;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    private final MarkEvidenceRepository evidenceRepository;
    private final MeterRegistry meterRegistry;
    @Setter
    @Value("${processing.similarity.min-score:0.6}")
    private double minSimilarity;
    /**
     * If true, apply a small rank-based weight to evidence contributions. When false, all
     * evidence contributions are treated equally (pure similarity aggregation).
     */
    @Setter
    @Value("${processing.similarity.use-rank-weighting:true}")
    private boolean useRankWeighting;
    @Value("${processing.similarity.parity-check.enabled:false}")
    private boolean parityCheckEnabled;
    @Value("${processing.similarity.parity-check.tolerance:0.001}")
    private double parityTolerance;
    @Value("${processing.similarity.parity-check.sample-size:3}")
    private int paritySampleSize;

    @PostConstruct
    void maybeRunParityCheck() {
        if (!parityCheckEnabled) return;
        try {
            runParityCheck();
        } catch (Exception e) {
            log.error("Similarity parity check failed: {}", e.getMessage());
            throw new IllegalStateException("Similarity parity check failed: " + e.getMessage(), e);
        }
    }

    private void runParityCheck() {
        // Sample a few processing rows and compare DB distance->similarity with Java cosine similarity.
        var page = org.springframework.data.domain.PageRequest.of(0, Math.max(1, paritySampleSize));
        var pageRes = evidenceRepository.findAllByEmbeddingIsNotNull(page);
        if (pageRes == null || pageRes.isEmpty()) return;
        for (var ev : pageRes.getContent()) {
            float[] emb = ev.getEmbedding();
            if (emb == null || emb.length == 0) continue;
            String vec = VectorUtils.toVectorLiteral(emb);
            // ask DB for the top match for this vector
            List<MarkEvidenceDistanceProjection> hits = evidenceRepository.findTopKSimilarEvidence(vec, 5);
            if (hits == null || hits.isEmpty()) continue;
            for (var p : hits) {
                if (p.getId().equals(ev.getId())) continue; // skip self
                Double distance = p.getDistance();
                if (distance == null) continue;
                double dbSim = 1.0 - distance;
                // fetch the target evidence embedding
                var opt = evidenceRepository.findById(p.getId());
                if (opt.isEmpty()) continue;
                var other = opt.get();
                Double javaCos = VectorUtils.cosineSimilarity(emb, other.getEmbedding());
                if (javaCos == null) continue;
                double diff = Math.abs(dbSim - javaCos);
                if (diff > parityTolerance) {
                    throw new IllegalStateException(String.format("DB/Java similarity mismatch: db=%.6f java=%.6f diff=%.6f (tolerance=%.6f).\nThis likely indicates inconsistent embedding normalization between DB and Java. Consider normalizing embeddings at ingestion or disabling parity check.", dbSim, javaCos, diff, parityTolerance));
                }
                break; // only check first non-self hit per sample
            }
        }
        log.info("Similarity parity check passed for {} samples", paritySampleSize);
    }

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} (submission={}) has no embedding, skipping similarity",
                    processing == null ? "null" : processing.getId(),
                    processing == null || processing.getSubmission() == null ? "null" : processing.getSubmission().getId());
            return List.of();
        }

        String vector = VectorUtils.toVectorLiteral(processing.getEmbedding());

        long start = System.nanoTime();
        List<MarkSuggestion> result;

        // Always use DB-backed similarity. Java in-process engine has been removed to avoid divergence
        // and maintain a single source of truth for similarity ranking.
        // Query returns id + occurrence id + distance. We then batch-load full entities to avoid N+1.
        List<MarkEvidenceDistanceProjection> hits = evidenceRepository.findTopKSimilarEvidence(vector, k);

        if (log.isDebugEnabled()) {
            log.debug("Top {} distances (first 5): {}", hits.size(), hits.stream()
                    .map(MarkEvidenceDistanceProjection::getDistance)
                    .limit(5)
                    .toList());
        }

        // Use mark id (Long) as key to avoid relying on entity equals()/hashCode().
        Map<Long, Double> scores = new HashMap<>();
        // Sum of weights per mark id — used to normalize weighted scores into a confidence value.
        Map<Long, Double> weightSums = new HashMap<>();

        if (hits.isEmpty()) {
            return List.of();
        }

        // Preserve ordering from the distance query by iterating over projections.

        // We rely on DB to compute cosine distance (pgvector). The projection returned by
        // findTopKSimilarEvidence contains the distance value. Convert distance -> similarity
        // and fetch only the lightweight (id, mark) mapping for aggregation.
        // Collect unique evidence ids to fetch lightweight mark mapping once.
        Set<UUID> idSet = hits.stream().map(MarkEvidenceDistanceProjection::getId).collect(Collectors.toSet());
        // Record how many evidence rows were considered by the DB query (pre-dedupe).
        try {
            meterRegistry.counter("processing.suggestions.considered.count", "engine", "db").increment(hits.size());
        } catch (Exception ignored) {}
        Map<UUID, Mark> markByEvidenceId = Map.of();
        // build a marksById lookup here to avoid an extra pass later
        Map<Long, Mark> marksById = new java.util.HashMap<>();
        if (!idSet.isEmpty()) {
            List<EvidenceMarkProjection> rows = evidenceRepository.findMarksByEvidenceIds(List.copyOf(idSet));
            // Use a safe merge function to tolerate duplicate keys in case of bad data or join issues.
            markByEvidenceId = rows.stream().collect(Collectors.toMap(
                    EvidenceMarkProjection::getId,
                    EvidenceMarkProjection::getMark,
                    (a, b) -> {
                        // If duplicates happen, keep the first but log so data issues can be investigated.
                        if (!Objects.equals(a.getId(), b.getId())) {
                            log.warn("Duplicate mark mapping encountered for same evidence id: keeping mark id {} over {}", a.getId(), b.getId());
                        }
                        return a;
                    }
            ));
            // Populate marksById from the rows in a single pass (avoid extra stream operations).
            for (EvidenceMarkProjection row : rows) {
                Mark m = row.getMark();
                if (m != null && m.getId() != null) {
                    marksById.putIfAbsent(m.getId(), m);
                }
            }
        }

        Set<UUID> seen = new HashSet<>();
        int seenCount = 0;
        int passing = 0;
        for (int idx = 0; idx < hits.size(); idx++) {
            MarkEvidenceDistanceProjection p = hits.get(idx);
            UUID id = p.getId();
            if (!seen.add(id)) {
                // Duplicate evidence id returned by the distance query — record and skip.
                try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "db").increment(); } catch (Exception ignored) {}
                continue; // dedupe
            }
            seenCount++;

            Mark mark = markByEvidenceId.get(id);
            if (mark == null) continue;
            Long markId = mark.getId();
            if (markId == null) continue;

            Double distance = p.getDistance();
            if (distance == null) continue;

            // Convert distance (cosine-distance) to similarity.
            // WARNING: this conversion relies on specific assumptions:
            //  - The DB uses the pgvector '<#>' operator which returns a cosine-based distance
            //  - Evidence embeddings are normalized (unit length)
            // Under these assumptions: similarity ~= 1.0 - distance.
            // If the operator, distance definition or normalization changes, this formula
            // will be incorrect and historical behavior may silently break. Keep this
            // in sync with the DB query implementation.
            // Note: minSimilarity threshold comparisons in DB and Java paths may differ
            // slightly due to floating-point and operator semantics; tests should cover
            // both code paths to ensure parity.
            double similarity = 1.0 - distance;
            if (similarity < minSimilarity) continue;
            passing++;

            // Weight contribution by rank (DB order) if enabled. Otherwise treat all hits equally.
            // NOTE: this ranking depends on DB ordering being stable for identical distances;
            // changes to the vector operator or planner may alter ordering and thus affect weights.
            double weight = useRankWeighting ? 1.0 / (1 + idx) : 1.0;
            double weighted = similarity * weight;

            scores.merge(markId, weighted, Double::sum);
            weightSums.merge(markId, weight, Double::sum);
        }

        long filteredLocal = Math.max(0, seenCount - passing);
        try { meterRegistry.counter("processing.suggestions.filtered.count", "engine", "db").increment(filteredLocal); } catch (Exception ignored) {}

            if (scores.isEmpty()) {
                result = List.of();
            } else {
                result = scores.entrySet().stream()
                // pre-filter entries with valid weight sums to avoid nulls in the mapping stage
                .filter(entry -> {
                    Long markId = entry.getKey();
                    Double weightSum = weightSums.get(markId);
                    if (weightSum == null || weightSum == 0.0) {
                        log.warn("Invariant violation: weightSum missing for mark id {}", markId);
                        return false;
                    }
                    // ensure we have the actual Mark entity for the id
                    if (!marksById.containsKey(markId)) {
                        log.warn("Missing Mark entity for id {} while aggregating scores", markId);
                        return false;
                    }
                    return true;
                })
                .map(entry -> {
                    Long markId = entry.getKey();
                    double totalScore = entry.getValue();
                    double weightSum = weightSums.get(markId);

                    // Normalize by the total weight to produce a weighted average confidence.
                    double confidence = totalScore / weightSum;

                    return MarkSuggestion.builder()
                            .processing(processing)
                            .mark(marksById.get(markId))
                            .confidence(confidence)
                            .build();
                })
                .sorted((a, b) -> {
                    int cmp = Double.compare(b.getConfidence(), a.getConfidence());
                    if (cmp != 0) return cmp;
                    Long aId = a.getMark() == null ? null : a.getMark().getId();
                    Long bId = b.getMark() == null ? null : b.getMark().getId();
                    if (aId == null && bId == null) return 0;
                    if (aId == null) return 1;
                    if (bId == null) return -1;
                    return aId.compareTo(bId);
                })
                .limit(k)
                .toList();
            }


        // filtered metric for DB branch was already recorded as filteredLocal inside the branch when applicable

        try {
            meterRegistry.summary("processing.suggestions.count", "engine", "db", "result", result.isEmpty() ? "empty" : "has")
                    .record(result.size());
        } catch (Exception e) {
            log.debug("Failed to record suggestions metric for processing {}: {}", processing.getId(), e.getMessage());
        }

        if (log.isDebugEnabled()) {
            Double topConfidence = result.stream().findFirst().map(MarkSuggestion::getConfidence).orElse(null);
            log.debug("Processing {} (submission={}) → {} suggestions (top confidence={})",
                    processing.getId(),
                    processing.getSubmission() == null ? "null" : processing.getSubmission().getId(),
                    result.size(),
                    topConfidence);
        }

        // Record similarity computation time
        try {
            long elapsedNanos = System.nanoTime() - start;
            meterRegistry.timer("processing.similarity.time", "engine", "db", "result", result.isEmpty() ? "empty" : "has")
                    .record(elapsedNanos, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            log.debug("Failed to record similarity timer for processing {}: {}", processing.getId(), e.getMessage());
        }

        return result;
    }
}
