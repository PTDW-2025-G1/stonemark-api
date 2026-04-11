package pt.estga.processing.services.similarity.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.CandidateEvidence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates aggregation by delegating responsibilities to focused components:
 * - CandidateGrouper: grouping & sorting
 * - ScoreCalculator: scoring, accumulation
 * - AggregationResultBuilder: final confidence computation, sorting and limiting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarkAggregator {

    private final CandidateGrouper grouper;
    private final ScoreCalculator calculator;
    private final AggregationResultBuilder resultBuilder;

    public AggregationResult aggregate(List<CandidateEvidence> candidates, Map<UUID, Mark> markByEvidenceId, int k, int missingMarkMappings) {
        Map<Long, Mark> marksById = new java.util.TreeMap<>();
        for (Mark m : markByEvidenceId.values()) {
            if (m == null || m.getId() == null) continue;
            Long id = m.getId();
            // Merge deterministically: if multiple Mark instances appear for the same
            // id (due to DB projection duplication), pick the one with the smallest
            // string representation to ensure stable selection regardless of input order.
            marksById.merge(id, m, (existing, candidate) -> {
                String es = Objects.toString(existing, "");
                String cs = Objects.toString(candidate, "");
                return cs.compareTo(es) < 0 ? candidate : existing;
            });
        }

        var contributionsByMark = grouper.groupAndSort(candidates, markByEvidenceId);
        var state = calculator.compute(contributionsByMark);
        return resultBuilder.build(state, marksById, k, missingMarkMappings);
    }
}
