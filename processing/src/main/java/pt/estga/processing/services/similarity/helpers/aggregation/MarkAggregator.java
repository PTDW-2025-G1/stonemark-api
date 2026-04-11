package pt.estga.processing.services.similarity.helpers.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.CandidateEvidence;

import java.util.List;
import java.util.Map;
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

    public AggregationResult aggregate(List<CandidateEvidence> candidates, Map<UUID, Mark> markByEvidenceId, int k) {
        // Build marksById map deterministically and tolerate duplicates
        Map<Long, Mark> marksById = new java.util.TreeMap<>();
        for (Mark m : markByEvidenceId.values()) {
            if (m == null || m.getId() == null) continue;
            marksById.putIfAbsent(m.getId(), m);
        }

        var contributionsByMark = grouper.groupAndSort(candidates, markByEvidenceId);
        var state = calculator.compute(contributionsByMark);
        return resultBuilder.build(state, marksById, k);
    }
}
