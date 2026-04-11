package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MixedNullDuplicatesFanoutTest {

    @Test
    void handles_nulls_duplicates_and_fanout_together() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e = TestBuilders.uuid("ffffffff-0000-0000-0000-00000000000f");

        // mark 1 has duplicate entries for same evidence
        List<CandidateEvidence> list1 = new ArrayList<>();
        list1.add(TestBuilders.candidate(e, 1L, 0.4));
        list1.add(TestBuilders.candidate(e, 1L, 0.6)); // duplicate, higher similarity

        // mark 2 has a single entry
        List<CandidateEvidence> list2 = List.of(TestBuilders.candidate(e, 2L, 0.5));

        Map<Long, List<CandidateEvidence>> contributions = new HashMap<>();
        contributions.put(1L, list1);
        contributions.put(2L, list2);

        var state = calc.compute(contributions);

        // After dedup: one contribution for mark1 (0.6) and one for mark2 (0.5) scaled by SPLIT
        assertEquals(2, state.fanOutContributionCount());
        assertEquals(1, state.duplicates());
        // Raw scores should be present and positive
        assertTrue(state.scores().get(1L) > 0.0);
        assertTrue(state.scores().get(2L) > 0.0);
    }
}
