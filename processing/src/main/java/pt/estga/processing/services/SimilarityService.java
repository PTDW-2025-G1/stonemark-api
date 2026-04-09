package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.shared.utils.VectorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    private final MarkEvidenceRepository evidenceRepository;
    // Tunable threshold to filter out poor matches. Configurable via application properties.
    @Value("${processing.similarity.max-distance:0.8}")
    private double distanceThreshold;
    private final MarkOccurrenceRepository occurrenceRepository;

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} has no embedding, skipping similarity", processing == null ? "null" : processing.getId());
            return List.of();
        }

        String vector = VectorUtils.toVectorLiteral(processing.getEmbedding());

        // Query returns id + occurrence id + distance. We then batch-load full entities to avoid N+1.
        List<MarkEvidenceDistanceProjection> hits = evidenceRepository.findTopKSimilarEvidence(vector, null, k);

        if (log.isDebugEnabled()) {
            log.debug("Top {} distances (first 5): {}", hits.size(), hits.stream()
                    .map(MarkEvidenceDistanceProjection::getDistance)
                    .limit(5)
                    .toList());
        }

        Map<Mark, Double> scores = new HashMap<>();
        // Sum of weights per mark — used to normalize weighted scores into a confidence value.
        Map<Mark, Double> weightSums = new HashMap<>();

        if (hits.isEmpty()) {
            return List.of();
        }

        // Preserve ordering from the distance query by iterating over projections.

        // Instead of loading full MarkEvidence entities, load only occurrences with their marks
        // using the occurrence ids returned by the projection. This avoids over-fetching.
        List<Long> occurrenceIds = hits.stream()
                .map(MarkEvidenceDistanceProjection::getOccurrenceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Mark> markByOccurrenceId = Map.of();
        if (!occurrenceIds.isEmpty()) {
            List<pt.estga.mark.entities.MarkOccurrence> occurrences = occurrenceRepository.findAllWithMarkByIdIn(occurrenceIds);
            markByOccurrenceId = occurrences.stream().collect(Collectors.toMap(pt.estga.mark.entities.MarkOccurrence::getId, pt.estga.mark.entities.MarkOccurrence::getMark));
        }

        Set<UUID> seen = new HashSet<>();
        for (int idx = 0; idx < hits.size(); idx++) {
            MarkEvidenceDistanceProjection p = hits.get(idx);
            UUID id = p.getId();
            // Deduplicate repeated evidence ids if present
            if (!seen.add(id)) {
                continue;
            }

            // We avoid loading the full MarkEvidence entity. Use occurrenceId from projection to find the mark.
            Long occurrenceId = p.getOccurrenceId();
            if (occurrenceId == null) {
                continue;
            }

            Mark mark = markByOccurrenceId.get(occurrenceId);
            if (mark == null) {
                continue;
            }


            Double distance = p.getDistance();
            if (distance == null) {
                continue;
            }

            // Apply simple quality filter to drop poor matches (tunable threshold).
            if (distance > distanceThreshold) {
                continue;
            }

            // Convert distance to similarity. Use exponential decay for sharper separation.
            double similarity = Math.exp(-distance);

            // Weight contribution by rank (DB order). This gives higher-ranked evidence more influence.
            double weight = 1.0 / (1 + idx); // idx==0 => weight 1.0
            double weighted = similarity * weight;

            scores.merge(mark, weighted, Double::sum);
            weightSums.merge(mark, weight, Double::sum);
        }

        if (scores.isEmpty()) {
            return List.of();
        }

        return scores.entrySet().stream()
                .map(entry -> {
                    Mark mark = entry.getKey();
                    double totalScore = entry.getValue();
                    Double weightSum = weightSums.get(mark);
                    if (weightSum == null || weightSum == 0.0) {
                        // This should never happen — signal an invariant violation so it can be investigated.
                        log.warn("Invariant violation: weightSum missing for mark {}", mark.getId());
                        return null;
                    }

                    // Normalize by the total weight to produce a weighted average confidence.
                    double confidence = totalScore / weightSum;

                    return MarkSuggestion.builder()
                            .processing(processing)
                            .mark(mark)
                            .confidence(confidence)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(5)
                .toList();
    }
}
