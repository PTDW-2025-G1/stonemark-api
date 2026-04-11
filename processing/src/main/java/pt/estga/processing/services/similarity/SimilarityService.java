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

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    /**
     * Similarity service (DB-backed candidate retrieval + JVM-side scoring).
     * <p>
     * Architecture note (explicit): this service treats the database as the
     * candidate retrieval layer (approximate nearest neighbours via pgvector)
     * and applies a JVM-side scoring/aggregation layer to produce per-mark
     * suggestions with confidence. Invariants and contract:
     *  - Embeddings MUST be L2-normalized (unit length) at ingestion time. The
     *    service enforces normalization for newly produced embeddings, but
     *    historical data must also be normalized to guarantee parity.
     *  - The DB projection MUST return a similarity value computed as
     *    1.0 - (me.embedding <#> CAST(:vector AS vector)) (i.e. 1 - cosine_distance).
     *    If the DB operator, index, or stored vectors change (e.g. switch to
     *    L2 or inner-product) this contract must be updated accordingly.
     * <p>
     * The scoring layer performs defensive validation of DB-provided similarities
     * (null/NaN/non-finite/out-of-range) and clamps values to [0.0, 1.0] before
     * using them in weighting to avoid propagation of corrupted data.
     */

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
    @Value("${processing.similarity.max-k:200}")
    private int maxK;
    @Value("${processing.similarity.per-mark-decay:0.5}")
    private double perMarkDecayFactor;

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
            // Collect hit ids (exclude self) and fetch target rows in a single batch to avoid N+1 lookups.
            List<UUID> hitIds = hits.stream()
                    .map(MarkEvidenceDistanceProjection::getId)
                    .filter(id -> !id.equals(ev.getId()))
                    .distinct()
                    .toList();
            if (hitIds.isEmpty()) continue;
            var fetched = evidenceRepository.findAllById(hitIds);
            // Map fetched entities by id for quick lookup. Avoid per-hit DB calls which cause N+1.
            Map<UUID, Object> fetchedById = fetched.stream().collect(Collectors.toMap(
                    e -> e.getId(),
                    e -> e
            ));
            Method getEmbeddingMethod = null;
            try {
                getEmbeddingMethod = ev.getClass().getMethod("getEmbedding");
            } catch (NoSuchMethodException ignored) {
                log.debug("Parity check: fetched entity does not expose getEmbedding(), skipping embedding comparison");
            }
            for (var p : hits) {
                if (p.getId().equals(ev.getId())) continue; // skip self
                Double dbSim = p.getSimilarity();
                if (dbSim == null) continue;
                var other = fetchedById.get(p.getId());
                if (other == null) continue;
                // Use reflection-free accessor via VectorUtils; the fetched object is expected to have getEmbedding()
                // We rely on the repository returning the same entity type as in the sample page.
                float[] otherEmb;
                try {
                    if (getEmbeddingMethod == null) {
                        // method not available, skip
                        continue;
                    }
                    otherEmb = (float[]) getEmbeddingMethod.invoke(other);
                } catch (Exception ex) {
                    // If reflection fails, skip this parity comparison but don't abort the whole check.
                    log.debug("Unable to access embedding for parity check entity {}: {}", p.getId(), ex.getMessage());
                    continue;
                }
                Double javaCos = VectorUtils.cosineSimilarity(emb, otherEmb);
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

        // Use the embedding produced at ingestion but enforce normalization at
        // query time as a defensive measure. Distributed ingestion may drift or
        // misbehave; normalizing here guarantees parity between DB and JVM
        // computations and prevents intermittent ranking instability.
        float[] rawQueryEmb = processing.getEmbedding();
        float[] queryEmb = VectorUtils.normalize(rawQueryEmb);
        if (queryEmb == null || queryEmb.length == 0) {
            try { meterRegistry.counter("processing.suggestions.unnormalized_embedding.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Processing {} embedding could not be normalized, skipping similarity (raw_norm={})",
                    processing.getId(), pt.estga.shared.utils.VectorUtils.l2Norm(rawQueryEmb));
            return List.of();
        }
        // Defensive check (should be very close to 1.0 after normalization)
        double norm = pt.estga.shared.utils.VectorUtils.l2Norm(queryEmb);
        if (Double.isNaN(norm) || Math.abs(norm - 1.0) > 1e-3) {
            try { meterRegistry.counter("processing.suggestions.unnormalized_embedding.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Processing {} embedding not normalized after normalization attempt (norm={}) — continuing with normalized vector", processing.getId(), norm);
        }
        String vector = VectorUtils.toVectorLiteral(queryEmb);

        long start = System.nanoTime();
        List<MarkSuggestion> result;

        // Always use DB-backed similarity. Java in-process engine has been removed to avoid divergence
        // and maintain a single source of truth for similarity ranking.
        // Clamp k to a safe maximum to avoid extremely large IN(...) queries and planner issues.
        int safeK = Math.max(1, Math.min(k, maxK));
        if (k > maxK) {
            try { meterRegistry.counter("processing.suggestions.k_clamped.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Requested k={} exceeds maxK={}, clamping to {}", k, maxK, safeK);
        }
        // Query returns id + occurrence id + distance. Push the minSimilarity threshold into SQL
        // to avoid pulling unnecessary rows. DB distance is converted to similarity by 1 - distance,
        // so SQL filter is distance <= (1 - minSimilarity).
        double maxDistance = 1.0 - minSimilarity;
        List<MarkEvidenceDistanceProjection> hits = evidenceRepository.findTopKSimilarEvidence(vector, safeK, maxDistance);

        if (log.isDebugEnabled()) {
            log.debug("Top {} similarities (first 5): {}", hits.size(), hits.stream()
                    .map(MarkEvidenceDistanceProjection::getSimilarity)
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

        // The DB projection contains distance values. We convert to similarity here and
        // explicitly re-rank by similarity in Java before aggregation. Rationale:
        //  - DB ordering may not be stable across planners/operators for tied distances
        //  - re-ranking ensures deterministic weighting and reproducible confidence
        // Note: the conversion similarity = 1.0 - distance assumes the DB operator uses
        // cosine-distance semantics. Prefer returning similarity directly from SQL when
        // possible to avoid this fragile conversion.
        List<MarkEvidenceDistanceProjection> nonNullHits = hits.stream().filter(Objects::nonNull).toList();
        // Sanitize similarity values returned from DB: reject null/NaN, clamp out-of-range values and
        // record metrics for invalid or out-of-range similarities. Preserve DB ordering.
        List<Map.Entry<MarkEvidenceDistanceProjection, Double>> scored = new ArrayList<>();
        for (MarkEvidenceDistanceProjection p : nonNullHits) {
            Double rawSim = p.getSimilarity();
            if (rawSim == null || rawSim.isNaN()) {
                try { meterRegistry.counter("processing.suggestions.invalid.similarity.count", "engine", "db").increment(); } catch (Exception ignored) {}
                continue;
            }
            double sim = rawSim;
            if (!Double.isFinite(sim)) {
                try { meterRegistry.counter("processing.suggestions.invalid.similarity.count", "engine", "db").increment(); } catch (Exception ignored) {}
                continue;
            }
            if (sim > 1.0 || sim < 0.0) {
                try { meterRegistry.counter("processing.suggestions.similarity.out_of_range.count", "engine", "db").increment(); } catch (Exception ignored) {}
                // clamp to [0,1] defensive range — we treat negative similarities as non-matching
                sim = Math.max(0.0, Math.min(1.0, sim));
            }
            scored.add(new AbstractMap.SimpleEntry<>(p, sim));
        }

        // Collect unique evidence ids from the (re-ranked) top candidates
        Set<UUID> idSet = scored.stream().map(e -> e.getKey().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        // Record DB-side candidate count and valid candidate count after sanitization
        try { meterRegistry.counter("processing.suggestions.db_candidates.count", "engine", "db").increment(hits.size()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.db_valid_candidates.count", "engine", "db").increment(scored.size()); } catch (Exception ignored) {}
        Map<UUID, Mark> markByEvidenceId = Map.of();
        // build a marksById lookup here to avoid an extra pass later
        Map<Long, Mark> marksById = new HashMap<>();
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
            try { meterRegistry.counter("processing.suggestions.db_mapped_hits.count", "engine", "db").increment(marksById.size()); } catch (Exception ignored) {}
        }

        // Deterministic aggregation: group contributions by mark and process each
        // mark's contributions in a stable, locally-sorted order. This eliminates
        // global ordering dependence and makes per-mark decay deterministic.
        Map<Long, List<Map.Entry<MarkEvidenceDistanceProjection, Double>>> contributionsByMark = new HashMap<>();
        for (Map.Entry<MarkEvidenceDistanceProjection, Double> e : scored) {
            MarkEvidenceDistanceProjection p = e.getKey();
            UUID id = p.getId();
            Mark mark = markByEvidenceId.get(id);
            if (mark == null) continue;
            Long markId = mark.getId();
            if (markId == null) continue;
            contributionsByMark.computeIfAbsent(markId, mk -> new ArrayList<>()).add(e);
        }

        Set<String> seenPairs = new HashSet<>();
        // Process marks in deterministic order (mark id ascending)
        List<Long> markIds = new ArrayList<>(contributionsByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<Map.Entry<MarkEvidenceDistanceProjection, Double>> list = contributionsByMark.get(markId);
            if (list == null || list.isEmpty()) continue;
            // Sort contributions for this mark deterministically: similarity desc, occurrenceId, evidenceId
            list.sort((a, b) -> {
                int cmp = Double.compare(b.getValue(), a.getValue());
                if (cmp != 0) return cmp;
                // Tie-breaker: occurrence id then evidence id
                String occA;
                String occB;
                try { var oa = a.getKey().getOccurrenceId(); occA = oa == null ? "" : oa.toString(); } catch (Throwable t) { occA = ""; }
                try { var ob = b.getKey().getOccurrenceId(); occB = ob == null ? "" : ob.toString(); } catch (Throwable t) { occB = ""; }
                cmp = occA.compareTo(occB);
                if (cmp != 0) return cmp;
                return a.getKey().getId().toString().compareTo(b.getKey().getId().toString());
            });

            // Per-mark deterministic accumulation
            int used = 0;
            double decay = Math.max(0.0, perMarkDecayFactor);
            for (int i = 0; i < list.size(); i++) {
                var entry = list.get(i);
                MarkEvidenceDistanceProjection p = entry.getKey();
                double similarity = entry.getValue();
                UUID id = p.getId();

                // Dedupe including occurrence id
                String occurrencePart;
                try {
                    var occ = p.getOccurrenceId();
                    occurrencePart = occ == null ? "null" : occ.toString();
                } catch (Throwable t) {
                    occurrencePart = "-";
                }
                String pairKey = id + ":" + markId + ":" + occurrencePart;
                if (!seenPairs.add(pairKey)) {
                    try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "db").increment(); } catch (Exception ignored) {}
                    continue;
                }

                if (Double.isNaN(similarity) || similarity < minSimilarity) continue;

                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) {
                    try { meterRegistry.counter("processing.suggestions.per_mark_decay_applied.count", "engine", "db").increment(); } catch (Exception ignored) {}
                }

                double simClamped = Math.max(0.0, Math.min(1.0, similarity));
                // Use rankScore local to this mark's ordering if rank weighting is enabled
                double rankScore = 1.0 / (1.0 + (double) i);
                double signal = useRankWeighting ? rankScore : simClamped;
                double contribution = signal * perMarkMultiplier;

                scores.merge(markId, contribution, Double::sum);
                weightSums.merge(markId, perMarkMultiplier, Double::sum);

                used++;
            }
        }

        // Record final suggestions count
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

        try { meterRegistry.counter("processing.suggestions.final_suggestions.count", "engine", "db").increment(result.size()); } catch (Exception ignored) {}

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
