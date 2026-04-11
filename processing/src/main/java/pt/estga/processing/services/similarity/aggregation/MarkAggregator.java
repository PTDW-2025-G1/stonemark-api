package pt.estga.processing.services.similarity.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

/**
 * Orchestrates aggregation by delegating responsibilities to focused components:
 * - CandidateGrouper: grouping & sorting
 * - ScoreCalculator: scoring, accumulation
 * - AggregationResultBuilder: final confidence computation, sorting and limiting
 * <p>
 * Deterministic ordering contract:
 * - Input evidence id order is preserved from the sanitizer's LinkedHashSet and
 *   from the `markByEvidenceId` LinkedHashMap built by the service.
 * - Grouping and per-mark iteration use TreeMap to guarantee ascending mark id
 *   order across runs. Sorting within groups is explicit and deterministic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarkAggregator {

    private final CandidateGrouper grouper;
    private final ScoreCalculator calculator;
    private final AggregationResultBuilder resultBuilder;

    public AggregationResult aggregate(List<CandidateEvidence> candidates, Map<UUID, List<Mark>> markByEvidenceId, int k, int missingMarkMappings) {
        // Build deterministic id->Mark map by flattening all mark lists. When multiple
        // Mark instances share the same id, pick one deterministically to avoid
        // order-dependent behavior.
        Map<Long, Mark> marksById = new TreeMap<>();
        if (markByEvidenceId != null) {
            for (List<Mark> list : markByEvidenceId.values()) {
                if (list == null) continue;
                for (Mark m : list) {
                    if (m == null || m.getId() == null) continue;
                    Long id = m.getId();
                    marksById.merge(id, m, (existing, candidate) -> {
                        String es = Objects.toString(existing, "");
                        String cs = Objects.toString(candidate, "");
                        return cs.compareTo(es) < 0 ? candidate : existing;
                    });
                }
            }
        }

        var contributionsByMark = grouper.groupAndSort(candidates, markByEvidenceId);
        // Build fan-out counts (evidenceId -> number of associated marks) for
        // correct scaling decisions in the ScoreCalculator.
        Map<UUID, Integer> fanOutCounts = new LinkedHashMap<>();
        if (markByEvidenceId != null) {
            for (Map.Entry<UUID, List<Mark>> e : markByEvidenceId.entrySet()) {
                List<Mark> list = e.getValue();
                fanOutCounts.put(e.getKey(), list == null ? 0 : list.size());
            }
        }

        var state = calculator.compute(contributionsByMark, fanOutCounts);
        return resultBuilder.build(state, marksById, k, missingMarkMappings);
    }
}
