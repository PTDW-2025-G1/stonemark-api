package pt.estga.processing.services.similarity.aggregation;

import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

public class FanOutResolver {

    public Map<java.util.UUID, Integer> computeFanOut(Map<Long, List<CandidateEvidence>> dedupedByMark) {
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
                evidenceToMarks.computeIfAbsent(id, k -> new HashSet<>()).add(markId);
            }
        }

        Map<java.util.UUID, Integer> fanOutCounts = new HashMap<>();
        for (Map.Entry<java.util.UUID, Set<Long>> e : evidenceToMarks.entrySet()) {
            fanOutCounts.put(e.getKey(), e.getValue().size());
        }
        return fanOutCounts;
    }
}
