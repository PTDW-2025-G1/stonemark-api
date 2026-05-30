package pt.estga.processing.services.similarity;

import org.junit.jupiter.api.Test;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.processing.config.policies.SanitizationPolicy;
import pt.estga.processing.models.SanitizationResult;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CandidateSanitizerBoundsTest {

    @Test
    void sanitizer_clamps_and_counts_invalids() {
        SanitizationPolicy policy = new SanitizationPolicy(0.2, 0.9);
        CandidateSanitizer sanitizer = new CandidateSanitizer(policy);

        var p1 = new MarkEvidenceDistanceDto(TestBuilders.uuid("00000000-0000-0000-0000-000000000001"), 1L, -0.5);
        var p2 = new MarkEvidenceDistanceDto(TestBuilders.uuid("00000000-0000-0000-0000-000000000002"), 2L, 0.5);
        var p3 = new MarkEvidenceDistanceDto(TestBuilders.uuid("00000000-0000-0000-0000-000000000003"), null, 1.2);
        var p4 = new MarkEvidenceDistanceDto(TestBuilders.uuid("00000000-0000-0000-0000-000000000004"), 3L, Double.NaN);
        var p5 = new MarkEvidenceDistanceDto(TestBuilders.uuid("00000000-0000-0000-0000-000000000005"), 4L, null);

        SanitizationResult res = sanitizer.sanitize(List.of(p1, p2, p3, p4, p5));

        assertEquals(5, res.rawHitCount());
        assertEquals(2, res.invalidSimilarityCount());
        assertEquals(2, res.outOfRangeCount());
        assertEquals(3, res.candidates().size());

        var similarityCounts = res.candidates().stream()
                .collect(Collectors.groupingBy(c -> Math.rint(c.similarity() * 100d) / 100d, Collectors.counting()));

        assertEquals(1L, similarityCounts.getOrDefault(Math.rint(0.2d * 100d) / 100d, 0L));
        assertEquals(1L, similarityCounts.getOrDefault(Math.rint(0.5d * 100d) / 100d, 0L));
        assertEquals(1L, similarityCounts.getOrDefault(Math.rint(0.9d * 100d) / 100d, 0L));

        var ids = res.idSet();
        assertTrue(ids.contains(TestBuilders.uuid("00000000-0000-0000-0000-000000000001")));
        assertTrue(ids.contains(TestBuilders.uuid("00000000-0000-0000-0000-000000000002")));
        assertTrue(ids.contains(TestBuilders.uuid("00000000-0000-0000-0000-000000000003")));
    }
}
