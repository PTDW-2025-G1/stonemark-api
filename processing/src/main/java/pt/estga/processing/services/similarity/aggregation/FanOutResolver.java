package pt.estga.processing.services.similarity.aggregation;

import lombok.extern.slf4j.Slf4j;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

@Slf4j
public class FanOutResolver {

    public Map<UUID, Integer> computeFanOut(Map<Long, List<CandidateEvidence>> dedupedByMark) {
        if (dedupedByMark == null) throw new IllegalArgumentException("dedupedByMark must not be null");
        Map<java.util.UUID, Set<Long>> evidenceToMarks = new HashMap<>();
        if (dedupedByMark.isEmpty()) return Collections.emptyMap();

        for (Map.Entry<Long, List<CandidateEvidence>> e : dedupedByMark.entrySet()) {
            Long markId = e.getKey();
            List<CandidateEvidence> list = e.getValue();
            if (list == null || list.isEmpty()) continue;
            for (CandidateEvidence ce : list) {
                if (ce == null) continue;
                java.util.UUID id = ce.evidenceId();
                if (id == null) continue;
                evidenceToMarks.computeIfAbsent(id, _ -> new HashSet<>()).add(markId);
            }
        }

        Map<java.util.UUID, Integer> fanOutCounts = new HashMap<>();
        for (Map.Entry<java.util.UUID, Set<Long>> e : evidenceToMarks.entrySet()) {
            fanOutCounts.put(e.getKey(), e.getValue().size());
        }
        return fanOutCounts;
    }

    /**
     * Resolve fan-out for the given evidence id from the provided map.
     * Returned value is guaranteed to be >= 1. Invalid or missing entries are
     * handled deterministically: missing -> 1; non-positive -> logged and clamped to 1.
     */
    public static int resolveFanOut(Map<java.util.UUID, Integer> fanOutCounts, java.util.UUID evidenceId) {
        if (fanOutCounts == null) return 1;
        Integer v = fanOutCounts.get(evidenceId);
        if (v == null) return 1;
        if (v <= 0) {
            log.warn("Invalid fanOut ({}) for evidence {} — clamping to 1", v, evidenceId);
            return 1;
        }
        return v;
    }
}
