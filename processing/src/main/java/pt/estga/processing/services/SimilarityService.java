package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Objects;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    private final MarkEvidenceRepository evidenceRepository;
    @Value("${processing.similarity.min-score:0.6}")
    private double minSimilarity;
    @Value("${processing.similarity.max-distance:0.8}")
    private double maxDistance;
    /**
     * If true, apply a small rank-based weight to evidence contributions. When false, all
     * evidence contributions are treated equally (pure similarity aggregation).
     */
    @Value("${processing.similarity.use-rank-weighting:true}")
    private boolean useRankWeighting;

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} has no embedding, skipping similarity", processing == null ? "null" : processing.getId());
            return List.of();
        }

        String vector = VectorUtils.toVectorLiteral(processing.getEmbedding());

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
        Map<UUID, Mark> markByEvidenceId = Map.of();
        if (!idSet.isEmpty()) {
            List<EvidenceMarkProjection> rows = evidenceRepository.findMarksByEvidenceIds(List.copyOf(idSet));
            // Use a safe merge function to tolerate duplicate keys in case of bad data or join issues.
            markByEvidenceId = rows.stream().collect(Collectors.toMap(
                    EvidenceMarkProjection::getId,
                    EvidenceMarkProjection::getMark,
                    (a, b) -> {
                        // If duplicates happen, keep the first but log so data issues can be investigated.
                        if (a != null && b != null && !Objects.equals(a.getId(), b.getId())) {
                            log.warn("Duplicate mark mapping encountered for same evidence id: keeping mark id {} over {}", a.getId(), b.getId());
                        }
                        return a;
                    }
            ));
        }

        Set<UUID> seen = new HashSet<>();
        for (int idx = 0; idx < hits.size(); idx++) {
            MarkEvidenceDistanceProjection p = hits.get(idx);
            UUID id = p.getId();
            if (!seen.add(id)) continue; // dedupe

            Long occurrenceId = p.getOccurrenceId();
            if (occurrenceId == null) continue;

            Mark mark = markByEvidenceId.get(id);
            if (mark == null) continue;
            Long markId = mark.getId();
            if (markId == null) continue;

            Double distance = p.getDistance();
            if (distance == null) continue;

            // Discard evidences where raw distance is above configured maximum (if set).
            if (distance > maxDistance) continue;

            // Convert distance (cosine-distance) to similarity. For pgvector '<#>' operator,
            // distance ~= 1 - cosine_similarity for normalized vectors.
            double similarity = 1.0 - distance;

            // Apply simple quality filter using a configurable minimum similarity
            if (similarity < minSimilarity) continue;

            // Weight contribution by rank (DB order) if enabled. Otherwise treat all hits equally.
            double weight = useRankWeighting ? 1.0 / (1 + idx) : 1.0;
            double weighted = similarity * weight;

            scores.merge(markId, weighted, Double::sum);
            weightSums.merge(markId, weight, Double::sum);
        }

        if (scores.isEmpty()) {
            return List.of();
        }

        // Build a markId -> Mark lookup from the lightweight mapping we fetched earlier.
        Map<Long, Mark> marksById = markByEvidenceId.values().stream()
                .filter(m -> m != null && m.getId() != null)
                .collect(Collectors.toMap(
                        Mark::getId,
                        m -> m,
                        (a, b) -> {
                            if (a != null && b != null && !Objects.equals(a.getId(), b.getId())) {
                                log.warn("Duplicate Mark entity for id {} when building marksById; keeping first", a.getId());
                            }
                            return a;
                        }
                ));

        return scores.entrySet().stream()
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
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(5)
                .toList();
    }
}
