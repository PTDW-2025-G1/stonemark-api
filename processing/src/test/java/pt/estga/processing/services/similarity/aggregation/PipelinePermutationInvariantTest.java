package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.MarkScore;
import pt.estga.processing.testutils.TestBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PipelinePermutationInvariantTest {

    @Test
    void pipeline_produces_same_result_for_shuffled_inputs() {
        CandidateGrouper grouper = new CandidateGrouper();
        ScoringPolicy scoringPolicy = new ScoringPolicy(true, 0.5, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(scoringPolicy);
        AggregationResultBuilder builder = new AggregationResultBuilder();
        MarkAggregator aggregator = new MarkAggregator(grouper, calc, builder);

        Random rnd = new Random(12345);

        // Build a modest dataset
        List<pt.estga.processing.models.CandidateEvidence> base = new ArrayList<>();
        Map<UUID, List<pt.estga.mark.entities.Mark>> markByEvidence = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            UUID eid = TestBuilders.uuidFromHex(String.format("%08x%08x", i, i));
            base.add(new pt.estga.processing.models.CandidateEvidence(eid, (long) i, rnd.nextDouble()));
            markByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + rnd.nextInt(5)))));
        }

        var reference = aggregator.aggregate(base, markByEvidence, 10, 0);

        // Run multiple random permutations of candidate list and insertion order of marks
        for (int iter = 0; iter < 20; iter++) {
            List<pt.estga.processing.models.CandidateEvidence> shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled, new Random(iter));
            Map<UUID, List<pt.estga.mark.entities.Mark>> varied = new LinkedHashMap<>();
            List<UUID> keys = new ArrayList<>(markByEvidence.keySet());
            Collections.shuffle(keys, new Random(iter));
            for (UUID k : keys) varied.put(k, markByEvidence.get(k));

            var r = aggregator.aggregate(shuffled, varied, 10, 0);
            assertEquals(reference.topScores().size(), r.topScores().size());

            // Assert identical mark ordering
            List<Long> expectedIds = reference.topScores().stream().map(MarkScore::markId).toList();
            List<Long> actualIds = r.topScores().stream().map(MarkScore::markId).toList();
            assertEquals(expectedIds, actualIds, "Top score mark ordering must be invariant to input permutation");

            // Assert confidences match within tolerance
            for (int i = 0; i < reference.topScores().size(); i++) {
                assertEquals(reference.topScores().get(i).confidence(), r.topScores().get(i).confidence(), 1e-6);
            }
        }
    }
}
