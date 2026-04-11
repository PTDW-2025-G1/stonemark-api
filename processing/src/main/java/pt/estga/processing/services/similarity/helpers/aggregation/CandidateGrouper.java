package pt.estga.processing.services.similarity.helpers.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

/**
 * Groups CandidateEvidence by mark id and sorts each group's evidences by
 * similarity and occurrence to produce a deterministic ordering for aggregation.
 */
@Component
public class CandidateGrouper {

    public Map<Long, List<CandidateEvidence>> groupAndSort(List<CandidateEvidence> candidates, Map<UUID, Mark> markByEvidenceId) {
        // Use a TreeMap so the grouping keys (mark ids) are iterated in sorted order,
        // ensuring deterministic behavior across runs regardless of input ordering.
        Map<Long, List<CandidateEvidence>> contributionsByMark = new TreeMap<>();
        if (candidates == null || candidates.isEmpty()) return contributionsByMark;

        for (CandidateEvidence c : candidates) {
            Mark mark = markByEvidenceId.get(c.evidenceId());
            if (mark == null || mark.getId() == null) continue;
            Long markId = mark.getId();
            contributionsByMark.computeIfAbsent(markId, k -> new ArrayList<>()).add(c);
        }

        // Sort each group's evidences deterministically
        for (List<CandidateEvidence> list : contributionsByMark.values()) {
            list.sort((a,b) -> {
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

        return contributionsByMark;
    }
}
