package pt.estga.processing.services.similarity.suggestionbuilder;

import org.springframework.stereotype.Component;
import pt.estga.processing.models.MarkScore;

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
        out.sort((a, b) -> {
            int cmp = Double.compare(b.confidence(), a.confidence());
            if (cmp != 0) return cmp;
            if (a.markId() == null && b.markId() == null) return 0;
            if (a.markId() == null) return 1;
            if (b.markId() == null) return -1;
            return a.markId().compareTo(b.markId());
        });
        return Collections.unmodifiableList(out);
    }
}
