package pt.estga.processing.services.similarity.aggregation;

import lombok.extern.slf4j.Slf4j;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

@Slf4j
public class FanOutResolver {
    // Minimum allowed fan-out. Centralized to avoid scattered Math.max(1, ...) uses.
    public static final int MIN_FANOUT = 1;

    /**
     * Clamp and validate a resolved fanOut value. Always returns >= MIN_FANOUT.
     * Invalid values are logged and deterministically clamped.
     */
    public static int clampFanOut(int value, UUID evidenceId) {
        if (value <= 0) {
            log.warn("Invalid fanOut ({}) for evidence {} — clamping to {}", value, evidenceId, MIN_FANOUT);
            return MIN_FANOUT;
        }
        return value;
    }

    public Map<UUID, Integer> computeFanOut(Map<Long, List<CandidateEvidence>> dedupedByMark) {
        if (dedupedByMark == null) throw new IllegalArgumentException("dedupedByMark must not be null");
        Map<UUID, Set<Long>> evidenceToMarks = new HashMap<>();
        if (dedupedByMark.isEmpty()) return Collections.emptyMap();

        for (Map.Entry<Long, List<CandidateEvidence>> e : dedupedByMark.entrySet()) {
            Long markId = e.getKey();
            List<CandidateEvidence> list = e.getValue();
            if (list == null || list.isEmpty()) continue;
            for (CandidateEvidence ce : list) {
                if (ce == null) continue;
                UUID id = ce.evidenceId();
                if (id == null) continue;
                evidenceToMarks.computeIfAbsent(id, _ -> new HashSet<>()).add(markId);
            }
        }

        Map<UUID, Integer> fanOutCounts = new HashMap<>();
        for (Map.Entry<UUID, Set<Long>> e : evidenceToMarks.entrySet()) {
            fanOutCounts.put(e.getKey(), e.getValue().size());
        }
        return fanOutCounts;
    }

    /**
     * Resolve fan-out for the given evidence id from the provided map.
     * Returned value is guaranteed to be >= 1. Invalid or missing entries are
     * handled deterministically: missing -> 1; non-positive -> logged and clamped to 1.
     */
    public static int resolveFanOut(Map<UUID, Integer> fanOutCounts, UUID evidenceId) {
        if (fanOutCounts == null) return MIN_FANOUT;
        Integer v = fanOutCounts.get(evidenceId);
        if (v == null) return MIN_FANOUT;
        return clampFanOut(v, evidenceId);
    }
}
