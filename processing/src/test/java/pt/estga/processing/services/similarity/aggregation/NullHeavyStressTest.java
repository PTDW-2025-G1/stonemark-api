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

public class NullHeavyStressTest {

    @Test
    void tolerate_malformed_inputs_and_do_not_crash() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "FULL");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e1 = TestBuilders.uuid("deadbeef-0000-0000-0000-0000000000de");

        List<CandidateEvidence> listA = new ArrayList<>();
        listA.add(null);
        listA.add(TestBuilders.candidate(e1, 1L, 0.5));
        listA.add(new CandidateEvidence(null, 2L, 0.4)); // null UUID

        // listB has a CandidateEvidence with NaN similarity
        List<CandidateEvidence> listB = new ArrayList<>();
        listB.add(TestBuilders.candidate(e1, 3L, Double.NaN));

        Map<Long, List<CandidateEvidence>> contributions = new HashMap<>();
        contributions.put(100L, listA);
        contributions.put(200L, listB);
        contributions.put(300L, null);

        var state = calc.compute(contributions);

        // No crash, and only valid finite similarity contributed
        assertNotNull(state);
        // mark 100 should have a positive score (from 0.5 contribution)
        assertTrue(state.scores().getOrDefault(100L, 0.0) > 0.0);
        // mark 200 has only NaN similarity -> should not produce a score
        assertFalse(state.scores().containsKey(200L));
    }
}
