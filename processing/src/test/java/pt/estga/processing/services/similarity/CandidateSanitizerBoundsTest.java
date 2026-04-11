package pt.estga.processing.services.similarity;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.SanitizationPolicy;
import pt.estga.processing.models.SanitizationResult;
import pt.estga.processing.testutils.TestMarkEvidenceDistanceProjection;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CandidateSanitizerBoundsTest {

    @Test
    void sanitizer_clamps_and_counts_invalids() {
        SanitizationPolicy policy = new SanitizationPolicy(0.2, 0.9);
        CandidateSanitizer sanitizer = new CandidateSanitizer(policy);

        var p1 = new TestMarkEvidenceDistanceProjection(TestBuilders.uuid("00000000-0000-0000-0000-000000000001"), 1L, -0.5); // out of range -> clamp to 0.2
        var p2 = new TestMarkEvidenceDistanceProjection(TestBuilders.uuid("00000000-0000-0000-0000-000000000002"), 2L, 0.5); // ok
        var p3 = new TestMarkEvidenceDistanceProjection(TestBuilders.uuid("00000000-0000-0000-0000-000000000003"), null, 1.2); // out of range -> clamp to 0.9
        var p4 = new TestMarkEvidenceDistanceProjection(TestBuilders.uuid("00000000-0000-0000-0000-000000000004"), 3L, Double.NaN); // invalid
        var p5 = new TestMarkEvidenceDistanceProjection(TestBuilders.uuid("00000000-0000-0000-0000-000000000005"), 4L, null); // invalid

        List.of(p1, p2, p3, p4, p5);

        SanitizationResult res = sanitizer.sanitize(List.of(p1, p2, p3, p4, p5));

        assertEquals(5, res.rawHitCount());
        assertEquals(2, res.invalidSimilarityCount());
        assertEquals(2, res.outOfRangeCount());
        assertEquals(3, res.candidates().size());

        // Check clamped values and preserved order
        var c = res.candidates();
        assertEquals(0.2d, c.get(0).similarity(), 1e-12);
        assertEquals(0.5d, c.get(1).similarity(), 1e-12);
        assertEquals(0.9d, c.get(2).similarity(), 1e-12);

        // idSet should preserve insertion order for remaining candidates
        var ids = res.idSet().toArray();
        assertEquals(TestBuilders.uuid("00000000-0000-0000-0000-000000000001"), ids[0]);
        assertEquals(TestBuilders.uuid("00000000-0000-0000-0000-000000000002"), ids[1]);
        assertEquals(TestBuilders.uuid("00000000-0000-0000-0000-000000000003"), ids[2]);
    }
}
