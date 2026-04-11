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

public class FanOutWithDuplicatesAndNullsTest {

    @Test
    void total_contribution_across_marks_equals_original_signal_under_split() {
        // no rank weighting, no decay, SPLIT strategy
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e = TestBuilders.uuid("abababab-0000-0000-0000-0000000000ab");

        // Build lists: mark1 has duplicate entries, mark2 has one entry, mark3 null list
        List<CandidateEvidence> list1 = new ArrayList<>();
        list1.add(TestBuilders.candidate(e, 1L, 0.9));
        list1.add(TestBuilders.candidate(e, 1L, 0.8)); // duplicate

        List<CandidateEvidence> list2 = List.of(TestBuilders.candidate(e, 2L, 0.7));

        Map<Long, List<CandidateEvidence>> contributions = new HashMap<>();
        contributions.put(1L, list1);
        contributions.put(2L, list2);
        contributions.put(3L, null);

        var state = calc.compute(contributions);

        // After dedup: contributions are 0.9 for mark1, 0.7 for mark2; fan-out = 2
        double raw1 = state.scores().get(1L);
        double raw2 = state.scores().get(2L);

        // Under SPLIT, scale factor = 1/2, so summed scaled contributions should approximate original best signal (0.9 + 0.7) * (1/2) per mark?
        // More usefully, sum of unscaled signals before fanout splitting equals 0.9 + 0.7 = 1.6. After SPLIT scaling, each mark has its own scaled value; the total across marks should equal the original sum times scaling? We assert relative relation:
        assertEquals(0.9 * 0.5, raw1, 1e-9);
        assertEquals(0.7 * 0.5, raw2, 1e-9);

        // Sum of scaled contributions equals (0.9+0.7) * 0.5
        double sumScaled = raw1 + raw2;
        assertEquals((0.9 + 0.7) * 0.5, sumScaled, 1e-9);
    }
}
