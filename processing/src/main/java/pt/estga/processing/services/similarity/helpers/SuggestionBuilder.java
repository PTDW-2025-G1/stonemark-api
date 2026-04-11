package pt.estga.processing.services.similarity.helpers;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.models.MarkScore;

import java.util.*;

@Service
public class SuggestionBuilder {

    /**
     * Build MarkSuggestion entities from aggregation outputs.
     * This is a small pure transformer: it does not access DB or emit metrics.
     */
    public List<MarkSuggestion> buildSuggestions(List<MarkScore> scores, MarkEvidenceProcessing processing) {
        if (scores == null || scores.isEmpty()) return List.of();

        // Deduplicate by mark id selecting the highest-confidence score per mark.
        Map<Long, MarkScore> bestByMark = getLongMarkScoreMap(scores);

        if (bestByMark.isEmpty()) return List.of();

        // Build suggestions and sort deterministically: confidence desc, markId asc
        List<MarkSuggestion> suggestions = new ArrayList<>(bestByMark.size());
        for (MarkScore ms : bestByMark.values()) {
            double conf = Math.max(0.0, Math.min(1.0, ms.confidence()));
            suggestions.add(MarkSuggestion.builder()
                    .processing(processing)
                    .mark(ms.mark())
                    .confidence(conf)
                    .build());
        }

        suggestions.sort((a, b) -> {
            int cmp = Double.compare(b.getConfidence(), a.getConfidence());
            if (cmp != 0) return cmp;
            Long aId = a.getMark() == null ? null : a.getMark().getId();
            Long bId = b.getMark() == null ? null : b.getMark().getId();
            if (aId == null && bId == null) return 0;
            if (aId == null) return 1;
            if (bId == null) return -1;
            return aId.compareTo(bId);
        });

        return Collections.unmodifiableList(suggestions);
    }

    private static @NonNull Map<Long, MarkScore> getLongMarkScoreMap(List<MarkScore> scores) {
        Map<Long, MarkScore> bestByMark = new TreeMap<>();
        for (MarkScore ms : scores) {
            if (ms == null) continue;
            Long markId = ms.mark() == null ? null : ms.mark().getId();
            if (markId == null) continue; // skip entries without a valid mark
            MarkScore existing = bestByMark.get(markId);
            if (existing == null || Double.compare(ms.confidence(), existing.confidence()) > 0) {
                bestByMark.put(markId, ms);
            }
        }
        return bestByMark;
    }
}
