package pt.estga.processing.testutils;

import pt.estga.processing.models.AggregationResult;

import static org.junit.jupiter.api.Assertions.*;

public final class AggregationAssertions {

    private AggregationAssertions() {}

    public static void assertValidAggregation(AggregationResult r) {
        assertNotNull(r);
        // No NaN confidences
        r.topScores().forEach(s -> assertFalse(Double.isNaN(s.confidence()), "NaN confidence for mark " + s.markId()));
        // Confidences within [0,1]
        r.topScores().forEach(s -> assertTrue(s.confidence() >= 0.0 && s.confidence() <= 1.0, "confidence out of bounds for " + s.markId()));
        // No negative raw scores or weights
        r.rawScores().values().forEach(v -> assertFalse(Double.isNaN(v) || v < 0.0));
        r.weightSums().values().forEach(v -> assertFalse(Double.isNaN(v) || v < 0.0));
        // Telemetry counters non-negative
        assertTrue(r.duplicates() >= 0);
        assertTrue(r.perMarkContributions() >= 0);
        assertTrue(r.fanOutContributionCount() >= 0);
    }
}
