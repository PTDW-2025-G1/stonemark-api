package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.shared.utils.VectorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final MarkEvidenceRepository evidenceRepository;

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        String vector = VectorUtils.toVectorLiteral(processing.getEmbedding());
        List<MarkEvidence> evidences = evidenceRepository.findTopKSimilarEvidence(vector, null, k);

        Map<Mark, Double> scores = new HashMap<>();
        Map<Mark, Integer> counts = new HashMap<>();

        for (MarkEvidence e : evidences) {

            if (e.getOccurrence() == null || e.getOccurrence().getMark() == null) {
                continue;
            }

            Mark mark = e.getOccurrence().getMark();

            double score = 1.0;

            scores.merge(mark, score, Double::sum);
            counts.merge(mark, 1, Integer::sum);
        }

        return scores.entrySet().stream()
                .map(entry -> {
                    Mark mark = entry.getKey();
                    double totalScore = entry.getValue();
                    int count = counts.get(mark);

                    double confidence = totalScore / count; // average

                    return MarkSuggestion.builder()
                            .processing(processing)
                            .mark(mark)
                            .confidence(confidence)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(5)
                .toList();
    }
}
