package pt.estga.processing.services.similarity.helpers;

import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
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
    public List<MarkSuggestion> buildSuggestions(List<MarkScore> scores,
                                                MarkEvidenceProcessing processing) {
        if (scores == null || scores.isEmpty()) return List.of();

        return scores.stream()
                .map(ms -> MarkSuggestion.builder()
                        .processing(processing)
                        .mark(ms.mark())
                        .confidence(ms.confidence())
                        .build())
                .toList();
    }
}
