package pt.estga.processing.services.similarity.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkAggregator {

    private final CandidateGrouper grouper;
    private final ScoreCalculator calculator;
    private final AggregationResultBuilder resultBuilder;

    public AggregationResult aggregate(List<CandidateEvidence> candidates, Map<UUID, List<Long>> markIdsByEvidenceId, int k, int missingMarkMappings) {
        Set<Long> validMarkIds = new TreeSet<>();
        if (markIdsByEvidenceId != null) {
            for (List<Long> ids : markIdsByEvidenceId.values()) {
                if (ids == null) continue;
                for (Long id : ids) {
                    if (id != null) validMarkIds.add(id);
                }
            }
        }

        var contributionsByMark = grouper.groupAndSort(candidates, markIdsByEvidenceId);
        var state = calculator.compute(contributionsByMark);
        return resultBuilder.build(state, validMarkIds, k, missingMarkMappings);
    }
}
