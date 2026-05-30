package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
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
    private static final int DATASET_SIZE = 20;
    private static final int TOP_K = 10;
    private static final double PERTURBATION_EPS = 1e-6;
    private static final double BASE_SIMILARITY = 0.5;
    private static final int PERMUTATION_ITERATIONS = 20;
    private static final int PERTURBATION_SEEDS = 10;
    private static final int PERTURBATION_ITERATIONS = 5;
    private static final int TIE_ITERATIONS = 10;
    private static final long BASE_RANDOM_SEED = 12345L;
    private static final long TIE_RANDOM_SEED = 424242L;
    private static final long PERTURBATION_BASE_SEED = 20000L;
    private static final double ASSERT_TOLERANCE = 1e-6;
    private static final boolean RANK_WEIGHTING_ENABLED = true;
    private static final double DECAY = 0.5;
    private static final String FANOUT_STRATEGY = "SPLIT";

    @Test
    void pipeline_produces_same_result_for_shuffled_inputs() {
        CandidateGrouper grouper = new CandidateGrouper();
        ScoringPolicy scoringPolicy = new ScoringPolicy(RANK_WEIGHTING_ENABLED, DECAY, FANOUT_STRATEGY);
        ScoreCalculator calc = new ScoreCalculator(scoringPolicy);
        AggregationResultBuilder builder = new AggregationResultBuilder();
        MarkAggregator aggregator = new MarkAggregator(grouper, calc, builder);

        Random rnd = new Random(BASE_RANDOM_SEED);

        List<CandidateEvidence> base = new ArrayList<>();
        Map<UUID, List<Long>> markIdsByEvidence = new LinkedHashMap<>();
        for (int i = 0; i < DATASET_SIZE; i++) {
            UUID eid = eid(i);
            base.add(new CandidateEvidence(eid, (long) i, rnd.nextDouble()));
            markIdsByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + rnd.nextInt(5)))));
        }

        var reference = aggregator.aggregate(base, markIdsByEvidence, TOP_K, 0);

        for (int iter = 0; iter < PERMUTATION_ITERATIONS; iter++) {
            List<CandidateEvidence> shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled, new Random(iter));
            Map<UUID, List<Long>> varied = new LinkedHashMap<>();
            List<UUID> keys = new ArrayList<>(markIdsByEvidence.keySet());
            Collections.shuffle(keys, new Random(iter));
            for (UUID k : keys) varied.put(k, markIdsByEvidence.get(k));

            var r = aggregator.aggregate(shuffled, varied, TOP_K, 0);
            assertEquals(reference.topScores().size(), r.topScores().size());

            List<Long> expectedIds = reference.topScores().stream().map(MarkScore::markId).toList();
            List<Long> actualIds = r.topScores().stream().map(MarkScore::markId).toList();
            assertEquals(expectedIds, actualIds, "Top score mark ordering must be invariant to input permutation");

            for (int i = 0; i < reference.topScores().size(); i++) {
                assertEquals(reference.topScores().get(i).confidence(), r.topScores().get(i).confidence(), ASSERT_TOLERANCE);
            }
        }

        for (int seed = 0; seed < PERTURBATION_SEEDS; seed++) {
            Random rnd2 = new Random(PERTURBATION_BASE_SEED + seed);
            List<CandidateEvidence> perturbed = new ArrayList<>();
            Map<UUID, List<Long>> perturbedMarkIdsByEvidence = new LinkedHashMap<>();
            for (int i = 0; i < DATASET_SIZE; i++) {
                UUID eid = eid(i);
                double sim = Math.max(0.0, Math.min(1.0, BASE_SIMILARITY + (rnd2.nextDouble() - 0.5) * PERTURBATION_EPS));
                perturbed.add(new CandidateEvidence(eid, (long) i, sim));
                perturbedMarkIdsByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + rnd2.nextInt(5)))));
            }
            var refP = aggregator.aggregate(perturbed, perturbedMarkIdsByEvidence, TOP_K, 0);
            for (int iter = 0; iter < PERTURBATION_ITERATIONS; iter++) {
                List<CandidateEvidence> shuffled = new ArrayList<>(perturbed);
                Collections.shuffle(shuffled, new Random(iter + seed));
                Map<UUID, List<Long>> varied = new LinkedHashMap<>();
                List<UUID> keys = new ArrayList<>(perturbedMarkIdsByEvidence.keySet());
                Collections.shuffle(keys, new Random(iter + seed));
                for (UUID k : keys) varied.put(k, perturbedMarkIdsByEvidence.get(k));
                var r = aggregator.aggregate(shuffled, varied, TOP_K, 0);
                List<Long> expectedIds = refP.topScores().stream().map(MarkScore::markId).toList();
                List<Long> actualIds = r.topScores().stream().map(MarkScore::markId).toList();
                assertEquals(expectedIds, actualIds, "Perturbed dataset ordering must be invariant");
            }
        }

        List<CandidateEvidence> tieBase = new ArrayList<>();
        Map<UUID, List<Long>> tieMarkIdsByEvidence = new LinkedHashMap<>();
        Random tieRnd = new Random(TIE_RANDOM_SEED);
        for (int i = 0; i < DATASET_SIZE; i++) {
            UUID eid = eid(i);
            tieBase.add(new CandidateEvidence(eid, (long) i, BASE_SIMILARITY));
            tieMarkIdsByEvidence.put(eid, List.of(TestBuilders.mark((long) (1 + tieRnd.nextInt(5)))));
        }
        var refTie = aggregator.aggregate(tieBase, tieMarkIdsByEvidence, TOP_K, 0);
        for (int iter = 0; iter < TIE_ITERATIONS; iter++) {
            List<CandidateEvidence> shuffled = new ArrayList<>(tieBase);
            Collections.shuffle(shuffled, new Random(iter));
            Map<UUID, List<Long>> varied = new LinkedHashMap<>();
            List<UUID> keys = new ArrayList<>(tieMarkIdsByEvidence.keySet());
            Collections.shuffle(keys, new Random(iter));
            for (UUID k : keys) varied.put(k, tieMarkIdsByEvidence.get(k));
            var r = aggregator.aggregate(shuffled, varied, TOP_K, 0);
            List<Long> expectedIds = refTie.topScores().stream().map(MarkScore::markId).toList();
            List<Long> actualIds = r.topScores().stream().map(MarkScore::markId).toList();
            assertEquals(expectedIds, actualIds, "Tie-heavy dataset ordering must be invariant");
        }
    }

    private static UUID eid(int i) {
        return TestUuidUtil.uuidFromHex(String.format("%08x%08x", i, i));
    }
}
