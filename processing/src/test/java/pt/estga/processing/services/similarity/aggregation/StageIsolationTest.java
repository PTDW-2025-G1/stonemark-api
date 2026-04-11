package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;
import pt.estga.processing.config.policies.ScoringPolicy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StageIsolationTest {

    @Test
    void normalization_clamps_and_filters() {
        CandidateNormalizationStage s = new CandidateNormalizationStage();
        var e = TestBuilders.uuid("abcdabcd-0000-0000-0000-0000000000ab");
        Map<Long, List<CandidateEvidence>> in = new HashMap<>();
        in.put(1L, List.of(new CandidateEvidence(e, 1L, -0.5), new CandidateEvidence(e, 2L, Double.NaN)));
        var out = s.normalize(in);
        assertTrue(out.containsKey(1L));
        // only the -0.5 should be clamped to 0.0 and NaN removed
        assertEquals(1, out.get(1L).size());
        assertEquals(0.0d, out.get(1L).get(0).similarity(), 1e-9);
    }

    @Test
    void dedup_preserves_max_similarity_and_counts_duplicates() {
        CandidateDeduplicationStage dd = new CandidateDeduplicationStage();
        var e = TestBuilders.uuid("abcdef01-0000-0000-0000-0000000000ab");
        Map<Long, List<CandidateEvidence>> in = new HashMap<>();
        in.put(42L, List.of(new CandidateEvidence(e, 1L, 0.2), new CandidateEvidence(e, 1L, 0.7)));
        var res = dd.deduplicate(in);
        assertEquals(1, res.duplicates());
        assertEquals(1, res.deduped().get(42L).size());
        assertEquals(0.7d, res.deduped().get(42L).get(0).similarity(), 1e-9);
    }

    @Test
    void scoring_stage_pure_function_example() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "SPLIT");
        MarkScoringStage scorer = new MarkScoringStage(policy);
        var e = TestBuilders.uuid("feedfeed-0000-0000-0000-0000000000fe");

        Map<Long, List<CandidateEvidence>> deduped = new HashMap<>();
        deduped.put(10L, List.of(new CandidateEvidence(e, 1L, 0.8)));
        deduped.put(20L, List.of(new CandidateEvidence(e, 2L, 0.6)));

        Map<java.util.UUID, Integer> fan = new HashMap<>();
        fan.put(e, 2);

        var out = scorer.score(deduped, fan);
        // With SPLIT and fan=2, raw for mark10 = 0.8 * 0.5
        assertEquals(0.8d * 0.5d, out.scores().get(10L), 1e-9);
        assertEquals(0.6d * 0.5d, out.scores().get(20L), 1e-9);
    }
}
