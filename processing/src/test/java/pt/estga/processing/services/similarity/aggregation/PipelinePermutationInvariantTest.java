package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.MarkScore;
import pt.estga.processing.testutils.TestUuidUtil;
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
        List<CandidateEvidence> base = new ArrayList<>();
        Map<UUID, List<Mark>> markByEvidence = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            UUID eid = TestUuidUtil.uuidFromHex(String.format("%08x%08x", i, i));
            base.add(new CandidateEvidence(eid, (long) i, rnd.nextDouble()));
            markByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + rnd.nextInt(5)))));
        }

        var reference = aggregator.aggregate(base, markByEvidence, 10, 0);

        // Run multiple random permutations of candidate list and insertion order of marks
        for (int iter = 0; iter < 20; iter++) {
            List<CandidateEvidence> shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled, new Random(iter));
            Map<UUID, List<Mark>> varied = new LinkedHashMap<>();
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

        // Additional stress: small random perturbations to similarities to detect
        // subtle nondeterministic ranking under near-equal scores.
        for (int seed = 0; seed < 10; seed++) {
            Random rnd2 = new Random(20000 + seed);
            double eps = 1e-6;
            List<CandidateEvidence> perturbed = new ArrayList<>();
            Map<UUID, List<Mark>> perturbedMarkByEvidence = new LinkedHashMap<>();
            for (int i = 0; i < 20; i++) {
                UUID eid = TestUuidUtil.uuidFromHex(String.format("%08x%08x", i, i));
                double baseSim = 0.5;
                double sim = Math.max(0.0, Math.min(1.0, baseSim + (rnd2.nextDouble() - 0.5) * eps));
                perturbed.add(new CandidateEvidence(eid, (long) i, sim));
                perturbedMarkByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + rnd.nextInt(5)))));
            }
            var refP = aggregator.aggregate(perturbed, perturbedMarkByEvidence, 10, 0);
            for (int iter = 0; iter < 5; iter++) {
                List<CandidateEvidence> shuffled = new ArrayList<>(perturbed);
                Collections.shuffle(shuffled, new Random(iter + seed));
                Map<UUID, List<Mark>> varied = new LinkedHashMap<>();
                List<UUID> keys = new ArrayList<>(perturbedMarkByEvidence.keySet());
                Collections.shuffle(keys, new Random(iter + seed));
                for (UUID k : keys) varied.put(k, perturbedMarkByEvidence.get(k));
                var r = aggregator.aggregate(shuffled, varied, 10, 0);
                List<Long> expectedIds = refP.topScores().stream().map(MarkScore::markId).toList();
                List<Long> actualIds = r.topScores().stream().map(MarkScore::markId).toList();
                assertEquals(expectedIds, actualIds, "Perturbed dataset ordering must be invariant");
            }
        }

        // Tie-heavy dataset: all similarities equal to exercise tie-break stability
        List<CandidateEvidence> tieBase = new ArrayList<>();
        Map<UUID, List<Mark>> tieMarkByEvidence = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            UUID eid = TestUuidUtil.uuidFromHex(String.format("%08x%08x", i, i));
            tieBase.add(new CandidateEvidence(eid, (long) i, 0.5));
            tieMarkByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + rnd.nextInt(5)))));
        }
        var refTie = aggregator.aggregate(tieBase, tieMarkByEvidence, 10, 0);
        for (int iter = 0; iter < 10; iter++) {
            List<CandidateEvidence> shuffled = new ArrayList<>(tieBase);
            Collections.shuffle(shuffled, new Random(iter));
            Map<UUID, List<Mark>> varied = new LinkedHashMap<>();
            List<UUID> keys = new ArrayList<>(tieMarkByEvidence.keySet());
            Collections.shuffle(keys, new Random(iter));
            for (UUID k : keys) varied.put(k, tieMarkByEvidence.get(k));
            var r = aggregator.aggregate(shuffled, varied, 10, 0);
            List<Long> expectedIds = refTie.topScores().stream().map(MarkScore::markId).toList();
            List<Long> actualIds = r.topScores().stream().map(MarkScore::markId).toList();
            assertEquals(expectedIds, actualIds, "Tie-heavy dataset ordering must be invariant");
        }
    }
}
