package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class OrderingInvarianceTest {

    @Test
    void different_input_orders_produce_same_aggregation() {
        // Use full MarkAggregator to produce AggregationResult with topScores
        CandidateGrouper grouper = new CandidateGrouper();
        ScoreCalculator calc = new ScoreCalculator(new ScoringPolicy(true, 0.5, "SPLIT"));
        AggregationResultBuilder builder = new AggregationResultBuilder();
        MarkAggregator aggregator = new MarkAggregator(grouper, calc, builder);

        UUID e1 = TestBuilders.uuid("11111111-1111-1111-1111-111111111111");
        UUID e2 = TestBuilders.uuid("22222222-2222-2222-2222-222222222222");

        List<CandidateEvidence> candidatesA = List.of(new CandidateEvidence(e1, 1L, 0.8), new CandidateEvidence(e2, 2L, 0.6));
        List<CandidateEvidence> candidatesB = List.of(new CandidateEvidence(e2, 2L, 0.6), new CandidateEvidence(e1, 1L, 0.8));

        Map<UUID, List<pt.estga.mark.entities.Mark>> mkA = new LinkedHashMap<>();
        mkA.put(e1, List.of(TestBuilders.mark(10L)));
        mkA.put(e2, List.of(TestBuilders.mark(20L)));

        Map<UUID, List<pt.estga.mark.entities.Mark>> mkB = new LinkedHashMap<>();
        mkB.put(e2, List.of(TestBuilders.mark(20L)));
        mkB.put(e1, List.of(TestBuilders.mark(10L)));

        var r1 = aggregator.aggregate(candidatesA, mkA, 10, 0);
        var r2 = aggregator.aggregate(candidatesB, mkB, 10, 0);

        assertEquals(r1.topScores().size(), r2.topScores().size());
        for (int i = 0; i < r1.topScores().size(); i++) {
            assertEquals(r1.topScores().get(i).markId(), r2.topScores().get(i).markId());
            assertEquals(r1.topScores().get(i).confidence(), r2.topScores().get(i).confidence(), 1e-6);
        }
    }
}
