package pt.estga.processing.services.similarity.suggestionbuilder;

import org.springframework.stereotype.Component;
import pt.estga.processing.models.MarkScore;
import pt.estga.processing.services.similarity.aggregation.AggregationResultBuilder;

import java.util.*;

@Component
public class MarkScoreSelector {

    /**
     * Select the best MarkScore per markId. Deterministic tie-breaker: higher confidence wins,
     * then smaller markId.
     */
    public List<MarkScore> selectBestPerMark(List<MarkScore> scores) {
        if (scores == null || scores.isEmpty()) return List.of();

        Map<Long, MarkScore> bestByMark = new TreeMap<>();
        for (MarkScore ms : scores) {
            if (ms == null) continue;
            Long markId = ms.markId();
            if (markId == null) continue;
            MarkScore existing = bestByMark.get(markId);
            if (existing == null || Double.compare(ms.confidence(), existing.confidence()) > 0) {
                bestByMark.put(markId, ms);
            }
        }

        List<MarkScore> out = new ArrayList<>(bestByMark.values());
        // Sort deterministically: confidence desc, markId asc
        AggregationResultBuilder.sortDeterministically(out);
        return Collections.unmodifiableList(out);
    }
}
