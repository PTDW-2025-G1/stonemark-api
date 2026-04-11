package pt.estga.processing.services.similarity.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

/**
 * Groups CandidateEvidence by mark id and sorts each group's evidences by
 * similarity and occurrence to produce a deterministic ordering for aggregation.
 * <p>
 * Note on fan-out: this component expands one evidence into contributions for
 * each associated mark (one evidence -> N contributions). The ScoreCalculator
 * must apply the chosen fan-out scaling (for example SPLIT 1/N) to avoid
 * accidental score inflation. This invariant is deliberate; do not change the
 * expansion behavior here without coordinating a corresponding change in the
 * scoring logic.
 */
@Component
public class CandidateGrouper {

    public Map<Long, List<CandidateEvidence>> groupAndSort(List<CandidateEvidence> candidates, Map<UUID, List<Mark>> markByEvidenceId) {
        // Use a TreeMap so the grouping keys (mark ids) are iterated in sorted order,
        // ensuring deterministic behavior across runs regardless of input ordering.
        Map<Long, List<CandidateEvidence>> contributionsByMark = new TreeMap<>();
        if (candidates == null || candidates.isEmpty()) return contributionsByMark;
        // Defensive: caller may pass null when no mark mappings are available.
        if (markByEvidenceId == null) return contributionsByMark;

        for (CandidateEvidence c : candidates) {
            List<Mark> marks = markByEvidenceId.get(c.evidenceId());
            if (marks == null || marks.isEmpty()) continue;
            // Expand contribution to all marks associated with this evidence id.
            for (Mark mark : marks) {
                if (mark == null || mark.getId() == null) continue;
                Long markId = mark.getId();
                contributionsByMark.computeIfAbsent(markId, k -> new ArrayList<>()).add(c);
            }
        }

        for (List<CandidateEvidence> list : contributionsByMark.values()) {
            sortGroupsEvidencesDeterministically(list);
        }

        return contributionsByMark;
    }

    static void sortGroupsEvidencesDeterministically(List<CandidateEvidence> list) {
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
}
