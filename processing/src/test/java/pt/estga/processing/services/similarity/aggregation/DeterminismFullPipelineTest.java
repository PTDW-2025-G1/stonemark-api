package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.testutils.TestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DeterminismFullPipelineTest {

    @Test
    void full_pipeline_is_deterministic_across_runs() {
        ScoringPolicy scoringPolicy = new ScoringPolicy(true, 0.5, "SPLIT");
        CandidateGrouper grouper = new CandidateGrouper();
        ScoreCalculator calculator = new ScoreCalculator(scoringPolicy);
        AggregationResultBuilder builder = new AggregationResultBuilder();
        MarkAggregator aggregator = new MarkAggregator(grouper, calculator, builder);

        UUID e1 = TestBuilders.uuid("11111111-1111-1111-1111-111111111111");
        UUID e2 = TestBuilders.uuid("22222222-2222-2222-2222-222222222222");

        List<CandidateEvidence> candidates = List.of(
                TestBuilders.candidate(e1, 100L, 0.8),
                TestBuilders.candidate(e2, 101L, 0.6)
        );

        // Build markByEvidenceId as a LinkedHashMap to simulate service behaviour
        Map<UUID, List<Mark>> markByEvidenceId = new LinkedHashMap<>();
        markByEvidenceId.put(e1, List.of(TestBuilders.mark(10L)));
        markByEvidenceId.put(e2, List.of(TestBuilders.mark(20L)));

        AggregationResult reference = aggregator.aggregate(candidates, markByEvidenceId, 10, 0);

        // Shuffle inputs and vary insertion order in markByEvidenceId to stress determinism
        Random rnd = new Random(12345);
        for (int iter = 0; iter < 10; iter++) {
            List<CandidateEvidence> shuffled = new ArrayList<>(candidates);
            Collections.shuffle(shuffled, rnd);
            Map<UUID, List<Mark>> variedMap = new LinkedHashMap<>();
            if ((iter & 1) == 0) {
                variedMap.put(e1, List.of(TestBuilders.mark(10L)));
                variedMap.put(e2, List.of(TestBuilders.mark(20L)));
            } else {
                variedMap.put(e2, List.of(TestBuilders.mark(20L)));
                variedMap.put(e1, List.of(TestBuilders.mark(10L)));
            }
            var r = aggregator.aggregate(shuffled, variedMap, 10, 0);
            assertEquals(reference.topScores().size(), r.topScores().size());
            for (int i = 0; i < reference.topScores().size(); i++) {
                var sRef = reference.topScores().get(i);
                var s = r.topScores().get(i);
                assertEquals(sRef.markId(), s.markId());
                // Allow a slightly relaxed numeric tolerance for floating differences across runs
                assertEquals(sRef.confidence(), s.confidence(), 1e-4);
            }
        }
    }
}
