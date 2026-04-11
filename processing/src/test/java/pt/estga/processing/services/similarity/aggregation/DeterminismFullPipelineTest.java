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

        AggregationResult r1 = aggregator.aggregate(candidates, markByEvidenceId, 10, 0);
        AggregationResult r2 = aggregator.aggregate(candidates, markByEvidenceId, 10, 0);

        assertEquals(r1.topScores().size(), r2.topScores().size());

        for (int i = 0; i < r1.topScores().size(); i++) {
            var s1 = r1.topScores().get(i);
            var s2 = r2.topScores().get(i);
            assertEquals(s1.markId(), s2.markId());
            assertEquals(s1.confidence(), s2.confidence(), 1e-6);
        }
    }
}
