package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import io.micrometer.core.instrument.MeterRegistry;
import pt.estga.shared.utils.VectorUtils;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaSimilarityEngine {

    private final MarkEvidenceRepository evidenceRepository;
    private final MeterRegistry meterRegistry;

    @Value("${processing.similarity.java.page-size:1000}")
    private int pageSize;

    /**
     * Compute suggestions in-JVM for the given processing. This method loads evidence
     * embeddings, computes cosine similarity, records filtered metrics and returns
     * the ordered list of suggestions.
     */
    public List<MarkSuggestion> computeSuggestions(
            MarkEvidenceProcessing processing,
            int k,
            double minSimilarity,
            boolean useRankWeighting
    ) {
        // Validate k
        if (k <= 0) return List.of();

        // Normalize processing embedding once to ensure consistent cosine computation
        float[] procEmb = processing.getEmbedding();
        double[] procNorm = VectorUtils.normalize(procEmb);
        if (procNorm == null || procNorm.length == 0) return List.of();

        // Stream evidence in pages and maintain a bounded min-heap to keep top-k evidence by similarity.
        PriorityQueue<Map.Entry<MarkEvidence, Double>> heap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
        long considered = 0L;

        int page = 0;
        while (true) {
            Pageable pageable = PageRequest.of(page, Math.max(1, pageSize));
            Page<MarkEvidence> p = evidenceRepository.findAllByEmbeddingIsNotNull(pageable);
            if (p == null || p.isEmpty()) break;

            for (MarkEvidence ev : p.getContent()) {
                // Defensive null checks: the domain model should ideally guarantee that
                // an evidence row has an embedding and an occurrence->mark mapping. These
                // guards protect the engine from dirty/partial data that can appear in
                // real databases; remove them only if the model invariants are enforced
                // at persistence time.
                if (ev == null) continue;
                if (ev.getEmbedding() == null || ev.getEmbedding().length == 0) continue;
                if (ev.getOccurrence() == null || ev.getOccurrence().getMark() == null || ev.getOccurrence().getMark().getId() == null) continue;

                considered++;

                double[] evNorm = VectorUtils.normalize(ev.getEmbedding());
                Double sim = VectorUtils.cosineSimilarity(procNorm, evNorm);
                if (sim == null) continue;

                Map.Entry<MarkEvidence, Double> entry = Map.entry(ev, sim);
                if (heap.size() < k) {
                    heap.offer(entry);
                } else {
                    Map.Entry<MarkEvidence, Double> smallest = heap.peek();
                    double smallestValue = smallest == null ? Double.NEGATIVE_INFINITY : smallest.getValue();
                    if (smallestValue < sim) {
                        heap.poll();
                        heap.offer(entry);
                    }
                }
            }

            if (!p.hasNext()) break;
            page++;
        }

        // Build sorted list of top-k candidates (descending by similarity)
        List<Map.Entry<MarkEvidence, Double>> filteredScored = new ArrayList<>(heap);
        filteredScored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Double> weightSums = new HashMap<>();
        Map<Long, Mark> marksById = new HashMap<>();

        Set<UUID> seen = new HashSet<>();
        long passing = 0L;
        for (int idx = 0; idx < filteredScored.size(); idx++) {
            var entry = filteredScored.get(idx);
            MarkEvidence ev = entry.getKey();
            double similarity = entry.getValue();

            // apply minSimilarity threshold after ranking (rank-first, threshold-second)
            if (Double.isNaN(similarity) || similarity < minSimilarity) continue;
            passing++;

            UUID id = ev.getId();
            if (!seen.add(id)) {
                // Duplicate evidence deduped by engine — record metric so we can detect DB join issues.
                try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "java").increment(); } catch (Exception ignored) {}
                continue;
            }

            pt.estga.mark.entities.Mark mark = ev.getOccurrence().getMark();
            if (mark == null || mark.getId() == null) continue;
            Long markId = mark.getId();

            double weight = useRankWeighting ? 1.0 / (1 + idx) : 1.0;
            double weighted = similarity * weight;

            scores.merge(markId, weighted, Double::sum);
            weightSums.merge(markId, weight, Double::sum);
            marksById.putIfAbsent(markId, mark);
        }

        long filtered = Math.max(0L, considered - passing);
        try {
            meterRegistry.counter("processing.suggestions.filtered.count", "engine", "java").increment(filtered);
        } catch (Exception ignored) {}
        // Record total considered rows (pre-filtering)
        try {
            meterRegistry.counter("processing.suggestions.considered.count", "engine", "java").increment(considered);
        } catch (Exception ignored) {}

        if (scores.isEmpty()) return List.of();

        List<MarkSuggestion> result = scores.entrySet().stream()
                .filter(entry -> {
                    Long markId = entry.getKey();
                    Double weightSum = weightSums.get(markId);
                    return weightSum != null && weightSum != 0.0 && marksById.containsKey(markId);
                })
                .map(entry -> {
                    Long markId = entry.getKey();
                    double totalScore = entry.getValue();
                    double weightSum = weightSums.get(markId);
                    double confidence = totalScore / weightSum;
                    return MarkSuggestion.builder()
                            .processing(processing)
                            .mark(marksById.get(markId))
                            .confidence(confidence)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(k)
                .toList();

        try {
            meterRegistry.counter("processing.suggestions.returned.count", "engine", "java").increment(result.size());
        } catch (Exception ignored) {}
        return result;
    }
}
