package pt.estga.processing.services.similarity;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.SanitizationPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.SanitizationResult;
import pt.estga.processing.testutils.TestMarkEvidenceDistanceProjection;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.stream.Collectors;

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

        SanitizationResult res = sanitizer.sanitize(List.of(p1, p2, p3, p4, p5));

        assertEquals(5, res.rawHitCount());
        assertEquals(2, res.invalidSimilarityCount());
        assertEquals(2, res.outOfRangeCount());
        assertEquals(3, res.candidates().size());

        // Instead of asserting positional ordering (which may change if upstream code evolves),
        // assert presence/counts of the clamped similarity values.
        // Group by quantized similarity (rounded to 1e-6) to avoid fragile exact-double equality
        var similarityCounts = res.candidates().stream()
                .collect(Collectors.groupingBy(c -> Math.round(c.similarity() * 1_000_000d), Collectors.counting()));

        assertEquals(1L, similarityCounts.getOrDefault(Math.round(0.2d * 1_000_000d), 0L));
        assertEquals(1L, similarityCounts.getOrDefault(Math.round(0.5d * 1_000_000d), 0L));
        assertEquals(1L, similarityCounts.getOrDefault(Math.round(0.9d * 1_000_000d), 0L));

        // idSet should contain the three remaining ids (order-preservation is an implementation detail and not asserted here)
        var ids = res.idSet();
        assertTrue(ids.contains(TestBuilders.uuid("00000000-0000-0000-0000-000000000001")));
        assertTrue(ids.contains(TestBuilders.uuid("00000000-0000-0000-0000-000000000002")));
        assertTrue(ids.contains(TestBuilders.uuid("00000000-0000-0000-0000-000000000003")));
    }
}
