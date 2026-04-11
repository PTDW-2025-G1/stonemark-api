package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.EvidenceKey;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.projections.EvidenceEmbeddingProjection;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.shared.utils.VectorUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    @Value("${processing.similarity.parity-check.async:true}")
    private boolean parityCheckAsync;
    @Value("${processing.embedding.dimension:0}")
    private int expectedEmbeddingDimension;
    @Value("${processing.similarity.max-k:200}")
    private int maxK;
    @Value("${processing.similarity.per-mark-decay:0.5}")
    private double perMarkDecayFactor;

    // Dedicated single-thread daemon executor used for the optional parity check so
    // it doesn't run on the ForkJoin common pool and has predictable threading.
    private static final ExecutorService parityExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "similarity-parity-check");
        t.setDaemon(true);
        return t;
    });

    /**
     * Small data holder for sanitized DB candidates.
     */
    private record SanitizedCandidates(List<Map.Entry<MarkEvidenceDistanceProjection, Double>> scored, Set<UUID> idSet, int rawHitCount) {}

    /**
     * Aggregation result containing per-mark scores, weight sums and the mark lookup.
     */
    private record AggregationResult(Map<Long, Double> scores, Map<Long, Double> weightSums, Map<Long, Mark> marksById) {}

    @PostConstruct
    void maybeRunParityCheck() {
        if (!parityCheckEnabled) return;
        if (parityCheckAsync) {
            // Run parity check asynchronously on a dedicated single-thread daemon
            // executor (avoid ForkJoinPool.commonPool) so startup work isn't blocked
            // and the check runs with predictable threading semantics.
            parityExecutor.submit(() -> {
                try {
                    runParityCheck();
                } catch (Exception e) {
                    log.error("Similarity parity check failed (async): {}", e.getMessage());
                }
            });
            return;
        }

        // Synchronous (fail-fast) behavior when async is disabled.
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
            // Fetch embeddings via projection (no entity hydration) to avoid reflection and N+1 queries.
            List<EvidenceEmbeddingProjection> fetched = evidenceRepository.findAllByIdIn(hitIds);
            Map<UUID, float[]> fetchedById = fetched.stream().collect(Collectors.toMap(
                    EvidenceEmbeddingProjection::getId,
                    EvidenceEmbeddingProjection::getEmbedding
            ));
            for (var p : hits) {
                if (p.getId().equals(ev.getId())) continue; // skip self
                Double dbSim = p.getSimilarity();
                if (dbSim == null) continue;
                float[] otherEmb = fetchedById.get(p.getId());
                if (otherEmb == null) continue;
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

    // --- Helper methods extracted from findSimilar for clarity and testability ---

    private Optional<String> validateAndNormalizeEmbedding(MarkEvidenceProcessing processing) {
        // Defensive normalization and dimension validation
        float[] rawQueryEmb = processing.getEmbedding();
        float[] queryEmb = VectorUtils.normalize(rawQueryEmb);
        if (queryEmb == null || queryEmb.length == 0) {
            try { meterRegistry.counter("processing.suggestions.unnormalized_embedding.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Processing {} embedding could not be normalized, skipping similarity (raw_norm={})",
                    processing.getId(), pt.estga.shared.utils.VectorUtils.l2Norm(rawQueryEmb));
            return Optional.empty();
        }

        if (expectedEmbeddingDimension > 0 && queryEmb.length != expectedEmbeddingDimension) {
            try { meterRegistry.counter("processing.suggestions.embedding_dimension_mismatch.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Processing {} embedding dimension {} does not match expected {} — skipping similarity",
                    processing.getId(), queryEmb.length, expectedEmbeddingDimension);
            return Optional.empty();
        }

        double norm = pt.estga.shared.utils.VectorUtils.l2Norm(queryEmb);
        if (Double.isNaN(norm) || Math.abs(norm - 1.0) > 1e-3) {
            try { meterRegistry.counter("processing.suggestions.unnormalized_embedding.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Processing {} embedding not normalized after normalization attempt (norm={}) — continuing with normalized vector", processing.getId(), norm);
        }

        return Optional.of(VectorUtils.toVectorLiteral(queryEmb));
    }

    private List<MarkEvidenceDistanceProjection> fetchCandidates(String vector, int k) {
        int safeK = Math.max(1, Math.min(k, maxK));
        if (k > maxK) {
            try { meterRegistry.counter("processing.suggestions.k_clamped.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Requested k={} exceeds maxK={}, clamping to {}", k, maxK, safeK);
        }
        double maxDistance = 1.0 - minSimilarity;
        maxDistance = Math.max(0.0, maxDistance);
        List<MarkEvidenceDistanceProjection> hits = evidenceRepository.findTopKSimilarEvidence(vector, safeK, maxDistance);
        if (hits == null) return List.of();
        if (log.isDebugEnabled()) {
            log.debug("Top {} similarities (first 5): {}", hits.size(), hits.stream()
                    .map(MarkEvidenceDistanceProjection::getSimilarity)
                    .limit(5)
                    .toList());
        }
        return hits;
    }

    private SanitizedCandidates sanitizeCandidates(List<MarkEvidenceDistanceProjection> hits) {
        if (hits == null || hits.isEmpty()) return new SanitizedCandidates(List.of(), Collections.emptySet(), 0);

        List<MarkEvidenceDistanceProjection> nonNullHits = hits.stream().filter(Objects::nonNull).toList();
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
                sim = Math.max(0.0, Math.min(1.0, sim));
            }
            scored.add(new AbstractMap.SimpleEntry<>(p, sim));
        }

        Set<UUID> idSet = scored.stream().map(e -> e.getKey().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        try { meterRegistry.counter("processing.suggestions.db_candidates.count", "engine", "db").increment(hits.size()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.db_valid_candidates.count", "engine", "db").increment(scored.size()); } catch (Exception ignored) {}

        return new SanitizedCandidates(scored, idSet, hits.size());
    }

    private AggregationResult aggregatePerMark(SanitizedCandidates sanitized) {
        List<Map.Entry<MarkEvidenceDistanceProjection, Double>> scored = sanitized.scored();
        Set<UUID> idSet = sanitized.idSet();

        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        Map<Long, Double> scoreComps = new TreeMap<>();
        Map<Long, Double> weightComps = new TreeMap<>();

        if (scored.isEmpty()) {
            return new AggregationResult(scores, weightSums, Collections.emptyMap());
        }

        Map<UUID, Mark> markByEvidenceId = Collections.emptyMap();
        Map<Long, Mark> marksById = new HashMap<>();
        if (!idSet.isEmpty()) {
            List<EvidenceMarkProjection> rows = evidenceRepository.findMarksByEvidenceIds(List.copyOf(idSet));
            markByEvidenceId = rows.stream().collect(Collectors.toMap(
                    EvidenceMarkProjection::getId,
                    EvidenceMarkProjection::getMark,
                    (a, b) -> {
                        if (!Objects.equals(a.getId(), b.getId())) {
                            log.warn("Duplicate mark mapping encountered for same evidence id: keeping mark id {} over {}", a.getId(), b.getId());
                        }
                        return a;
                    }
            ));
            for (EvidenceMarkProjection row : rows) {
                Mark m = row.getMark();
                if (m != null && m.getId() != null) {
                    marksById.putIfAbsent(m.getId(), m);
                }
            }
            try { meterRegistry.counter("processing.suggestions.db_mapped_hits.count", "engine", "db").increment(marksById.size()); } catch (Exception ignored) {}
        }

        Map<Long, List<Map.Entry<MarkEvidenceDistanceProjection, Double>>> contributionsByMark = new HashMap<>();
        for (Map.Entry<MarkEvidenceDistanceProjection, Double> e : scored) {
            MarkEvidenceDistanceProjection p = e.getKey();
            UUID id = p.getId();
            Mark mark = markByEvidenceId.get(id);
            if (mark == null) continue;
            Long markId = mark.getId();
            if (markId == null) continue;
            contributionsByMark.computeIfAbsent(markId, __ -> new ArrayList<>()).add(e);
        }

        Set<EvidenceKey> seenPairs = new HashSet<>();
        List<Long> markIds = new ArrayList<>(contributionsByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<Map.Entry<MarkEvidenceDistanceProjection, Double>> list = contributionsByMark.get(markId);
            if (list == null || list.isEmpty()) continue;
            list.sort((a, b) -> {
                int cmp = Double.compare(b.getValue(), a.getValue());
                if (cmp != 0) return cmp;
                Long occA;
                Long occB;
                try { occA = a.getKey().getOccurrenceId(); } catch (Throwable t) { occA = null; }
                try { occB = b.getKey().getOccurrenceId(); } catch (Throwable t) { occB = null; }
                if (occA == null && occB != null) return -1;
                if (occA != null && occB == null) return 1;
                if (occA != null) {
                    int cmpOcc = occA.compareTo(occB);
                    if (cmpOcc != 0) return cmpOcc;
                }
                return a.getKey().getId().toString().compareTo(b.getKey().getId().toString());
            });

            int used = 0;
            double decay = Math.max(0.0, perMarkDecayFactor);
            for (int i = 0; i < list.size(); i++) {
                var entry = list.get(i);
                MarkEvidenceDistanceProjection p = entry.getKey();
                double similarity = entry.getValue();
                UUID id = p.getId();

                Long occurrenceId;
                try { occurrenceId = p.getOccurrenceId(); } catch (Throwable t) { occurrenceId = null; }
                EvidenceKey key = new EvidenceKey(id, markId, occurrenceId);
                if (!seenPairs.add(key)) {
                    try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "db").increment(); } catch (Exception ignored) {}
                    continue;
                }

                if (Double.isNaN(similarity) || similarity < minSimilarity) continue;

                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) {
                    try { meterRegistry.counter("processing.suggestions.per_mark_decay_applied.count", "engine", "db").increment(); } catch (Exception ignored) {}
                }

                try { meterRegistry.counter("processing.suggestions.per_mark_contributions.count", "engine", "db").increment(); } catch (Exception ignored) {}

                double simClamped = Math.max(0.0, Math.min(1.0, similarity));
                double rankScore = 1.0 / (1.0 + (double) i);
                double signal = simClamped * (useRankWeighting ? rankScore : 1.0);
                double contribution = signal * perMarkMultiplier;

                double s = scores.getOrDefault(markId, 0.0);
                double sc = scoreComps.getOrDefault(markId, 0.0);
                double y = contribution - sc;
                double t = s + y;
                sc = (t - s) - y;
                s = t;
                scores.put(markId, s);
                scoreComps.put(markId, sc);

                double w = weightSums.getOrDefault(markId, 0.0);
                double wc = weightComps.getOrDefault(markId, 0.0);
                double wy = perMarkMultiplier - wc;
                double wt = w + wy;
                wc = (wt - w) - wy;
                w = wt;
                weightSums.put(markId, w);
                weightComps.put(markId, wc);

                used++;
            }
        }

        return new AggregationResult(scores, weightSums, marksById);
    }

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} (submission={}) has no embedding, skipping similarity",
                    processing == null ? "null" : processing.getId(),
                    processing == null || processing.getSubmission() == null ? "null" : processing.getSubmission().getId());
            return List.of();
        }

        // Validate and normalize embedding (returns vector literal) — extracted for clarity
        Optional<String> maybeVector = validateAndNormalizeEmbedding(processing);
        if (maybeVector.isEmpty()) return List.of();
        String vector = maybeVector.get();

        long start = System.nanoTime();
        List<MarkSuggestion> result;

        // Always use DB-backed similarity. Java in-process engine has been removed to avoid divergence
        // and maintain a single source of truth for similarity ranking.
        // Clamp k to a safe maximum to avoid extremely large IN(...) queries and planner issues.
        // Fetch DB candidates (keeps k-clamping and maxDistance logic)
        List<MarkEvidenceDistanceProjection> hits = fetchCandidates(vector, k);
        // Sanitize DB results and collect evidence ids
        SanitizedCandidates sanitized = sanitizeCandidates(hits);
        if (sanitized.scored().isEmpty()) return List.of();

        // Aggregate per-mark contributions (includes mapping evidence -> mark)
        AggregationResult aggregation = aggregatePerMark(sanitized);

        // Record final suggestions count (build from aggregation result)
        Map<Long, Double> scores = aggregation.scores();
        Map<Long, Double> weightSums = aggregation.weightSums();
        Map<Long, Mark> marksById = aggregation.marksById();

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
                // Quantize confidence to 1e-6 to improve determinism across runs and reduce
                // floating-point jitter when ranking / comparing results.
                double quantized = Math.round(confidence * 1_000_000d) / 1_000_000d;

                return MarkSuggestion.builder()
                        .processing(processing)
                        .mark(marksById.get(markId))
                        .confidence(quantized)
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
