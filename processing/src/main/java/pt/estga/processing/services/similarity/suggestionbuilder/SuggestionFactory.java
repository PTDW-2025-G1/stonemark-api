package pt.estga.processing.services.similarity.suggestionbuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.models.MarkScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SuggestionFactory {

    public List<MarkSuggestion> buildSuggestions(List<MarkScore> scores, MarkEvidenceProcessing processing) {
        if (scores == null || scores.isEmpty()) return List.of();

        List<MarkSuggestion> suggestions = new ArrayList<>(scores.size());
        for (MarkScore ms : scores) {
            double conf = Math.max(0.0, Math.min(1.0, ms.confidence()));
            suggestions.add(MarkSuggestion.builder()
                    .processing(processing)
                    .mark(ms.mark())
                    .confidence(conf)
                    .build());
        }

        // Already selected/sorted by selector; ensure deterministic order remains
        return Collections.unmodifiableList(suggestions);
    }
}
