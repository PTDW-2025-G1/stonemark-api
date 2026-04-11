package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests and locks the contract for fanOutContributionCount:
 * - It counts processed (deduplicated) contributions where fanOut > 1.
 */
public class FanOutContributionCountContractTest {

    @Test
    void fanout_count_counts_deduped_processed_contributions_with_fanout_greater_than_one() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e = TestBuilders.uuid("dddddddd-0000-0000-0000-00000000000d");

        // mark 10 has duplicate entries for the same evidence -> dedup reduces to one
        CandidateEvidence m10a = TestBuilders.candidate(e, 1L, 0.7);
        CandidateEvidence m10b = TestBuilders.candidate(e, 1L, 0.6);
        // mark 20 has one entry
        CandidateEvidence m20 = TestBuilders.candidate(e, 2L, 0.5);

        Map<Long, List<CandidateEvidence>> contributions = Map.of(
                10L, List.of(m10a, m10b),
                20L, List.of(m20)
        );

        var state = calc.compute(contributions);

        // After dedup, processed contributions for this evidence: one for mark10, one for mark20 => 2
        assertEquals(2, state.fanOutContributionCount());
        // duplicates should reflect one duplicate removed
        assertEquals(1, state.duplicates());
    }
}
