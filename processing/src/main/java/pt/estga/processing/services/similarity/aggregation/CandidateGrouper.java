package pt.estga.processing.services.similarity.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

@Component
public class CandidateGrouper {

    public Map<Long, List<CandidateEvidence>> groupAndSort(List<CandidateEvidence> candidates, Map<UUID, List<Long>> markIdsByEvidenceId) {
        Map<Long, List<CandidateEvidence>> contributionsByMark = new TreeMap<>();
        if (candidates == null || candidates.isEmpty()) return contributionsByMark;
        if (markIdsByEvidenceId == null) return contributionsByMark;

        for (CandidateEvidence c : candidates) {
            List<Long> markIds = markIdsByEvidenceId.get(c.evidenceId());
            if (markIds == null || markIds.isEmpty()) continue;
            for (Long markId : markIds) {
                if (markId == null) continue;
                contributionsByMark.computeIfAbsent(markId, k -> new ArrayList<>()).add(c);
            }
        }

        return contributionsByMark;
    }

    static void sortGroupsEvidencesDeterministically(List<CandidateEvidence> list) {
        list.sort((a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;

            int cmp = Double.compare(b.similarity(), a.similarity());
            if (cmp != 0) return cmp;
            Long oa = a.occurrenceId();
            Long ob = b.occurrenceId();
            if (oa == null && ob != null) return -1;
            if (oa != null && ob == null) return 1;
            if (oa != null) {
                int c = oa.compareTo(ob);
                if (c != 0) return c;
            }
            return a.evidenceId().toString().compareTo(b.evidenceId().toString());
        });
    }
}
