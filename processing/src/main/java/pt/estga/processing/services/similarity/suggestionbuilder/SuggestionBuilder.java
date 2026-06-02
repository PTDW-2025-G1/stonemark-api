package pt.estga.processing.services.similarity.suggestionbuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.models.MarkScore;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionBuilder {

    private final MarkScoreSelector selector;
    private final SuggestionFactory factory;

    public List<MarkSuggestion> buildSuggestions(List<MarkScore> scores, MarkEvidenceProcessing processing) {
        List<MarkScore> selected = selector.selectBestPerMark(scores);
        return factory.buildSuggestions(selected, processing);
    }
}
