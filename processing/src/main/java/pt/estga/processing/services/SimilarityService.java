package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkEvidenceRepository;
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
    @Value("${processing.similarity.min-score:0.2}")
    private double minSimilarity;

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

        // We will compute cosine similarity in Java. Fetch full MarkEvidence entities (includes embedding)
        // for the top-K hits and map them by id. This is slightly heavier but gives exact cosine similarity
        // and avoids relying on DB operator differences between environments.
        List<UUID> ids = hits.stream().map(MarkEvidenceDistanceProjection::getId).distinct().toList();
        Map<UUID, pt.estga.mark.entities.MarkEvidence> evidenceById = Map.of();
        if (!ids.isEmpty()) {
            List<pt.estga.mark.entities.MarkEvidence> evidences = evidenceRepository.findAllWithOccurrenceAndMarkByIdIn(ids);
            evidenceById = evidences.stream().collect(Collectors.toMap(pt.estga.mark.entities.MarkEvidence::getId, e -> e));
        }

        Set<UUID> seen = new HashSet<>();
        for (int idx = 0; idx < hits.size(); idx++) {
            MarkEvidenceDistanceProjection p = hits.get(idx);
            UUID id = p.getId();
            if (!seen.add(id)) continue; // dedupe

            pt.estga.mark.entities.MarkEvidence ev = evidenceById.get(id);
            if (ev == null) continue; // missing entity

            pt.estga.mark.entities.MarkOccurrence occ = ev.getOccurrence();
            if (occ == null) continue;
            Mark mark = occ.getMark();
            if (mark == null) continue;

            // Compute cosine similarity between processing embedding and evidence embedding
            float[] a = processing.getEmbedding();
            float[] b = ev.getEmbedding();
            double similarity = cosineSimilarity(a, b);
            if (Double.isNaN(similarity)) continue;

            // Apply simple quality filter using a configurable minimum similarity
            if (similarity < minSimilarity) continue;

            // Weight contribution by rank (DB order). Higher-ranked evidence has more influence.
            double weight = 1.0 / (1 + idx);
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

    // Helper: compute cosine similarity between two float[] embeddings
    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) return Double.NaN;
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            double va = a[i];
            double vb = b[i];
            dot += va * vb;
            na += va * va;
            nb += vb * vb;
        }
        if (na == 0.0 || nb == 0.0) return Double.NaN;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
