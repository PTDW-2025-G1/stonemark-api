package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.testutils.TestBuilders;
import pt.estga.processing.models.CandidateEvidence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FanOutSemanticTest {

    @Test
    void fanout_counts_reflect_original_graph_not_post_normalization() {
        FanOutResolver resolver = new FanOutResolver();

        var e = TestBuilders.uuid("cafebab1-0000-0000-0000-0000000000ca");

        // raw input: evidence appears in marks 1,2,3
        Map<Long, List<CandidateEvidence>> raw = new HashMap<>();
        raw.put(1L, List.of(new CandidateEvidence(e, 1L, 0.9)));
        raw.put(2L, List.of(new CandidateEvidence(e, 2L, Double.NaN))); // will be removed by normalization
        raw.put(3L, List.of(new CandidateEvidence(e, 3L, 0.8)));

        // fanout should be 3 when computed from raw input
        var fanRaw = resolver.computeFanOut(raw);
        assertEquals(3, fanRaw.get(e).intValue());

        // If normalization removed NaN, a post-normalization fanout would be 2 — which is incorrect
    }
}
