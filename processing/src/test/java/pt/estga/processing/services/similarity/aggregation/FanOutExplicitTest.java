package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FanOutExplicitTest {

    @Test
    void fanout_counts_are_explicit_and_dedup_does_not_reduce_fanout() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID eMulti = TestBuilders.uuid("aaaaaaa1-0000-0000-0000-000000000001");
        UUID eSingle = TestBuilders.uuid("bbbbbbb1-0000-0000-0000-000000000002");

        CandidateEvidence m1 = TestBuilders.candidate(eMulti, 1L, 0.8);
        CandidateEvidence m1dup = TestBuilders.candidate(eMulti, 1L, 0.7); // duplicate for mark10
        CandidateEvidence m2 = TestBuilders.candidate(eMulti, 2L, 0.6);
        CandidateEvidence m3 = TestBuilders.candidate(eMulti, 3L, 0.5);

        CandidateEvidence s1 = TestBuilders.candidate(eSingle, 10L, 0.9);

        Map<Long, List<CandidateEvidence>> contributions = Map.of(
                10L, List.of(m1, m1dup),
                20L, List.of(m2),
                30L, List.of(m3),
                40L, List.of(s1)
        );

        var state = calc.compute(contributions);

        // eMulti appears in 3 distinct marks; after dedup it should contribute once per mark => fanOutContributionCount increments by 3
        assertEquals(3, state.fanOutContributionCount());

        // eSingle appears in one mark -> should not contribute to fanOut counter
        // verify there is at least one score for mark 40
        assertTrue(state.scores().containsKey(40L));
    }
}
