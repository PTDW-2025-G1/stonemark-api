package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.testutils.AggregationAssertions;
import pt.estga.processing.testutils.TestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FuzzInvariantsTest {

    @Test
    void fuzz_randomized_inputs_respect_invariants_and_determinism_for_seed() {
        CandidateGrouper grouper = new CandidateGrouper();
        ScoringPolicy scoringPolicy = new ScoringPolicy(true, 0.6, "SPLIT");
        ScoreCalculator calculator = new ScoreCalculator(scoringPolicy);
        AggregationResultBuilder builder = new AggregationResultBuilder();
        MarkAggregator aggregator = new MarkAggregator(grouper, calculator, builder);

        long seed = 42L;
        Random rnd = new Random(seed);

        Map<UUID, List<pt.estga.mark.entities.Mark>> markByEvidenceId = new LinkedHashMap<>();
        List<pt.estga.processing.models.CandidateEvidence> candidates = new ArrayList<>();

        int evidCount = 50;
        for (int i = 0; i < evidCount; i++) {
            UUID eid = UUID.randomUUID();
            int marks = 1 + rnd.nextInt(5);
            List<pt.estga.mark.entities.Mark> mlist = new ArrayList<>();
            for (int k = 0; k < marks; k++) mlist.add(TestBuilders.mark((long) (1 + rnd.nextInt(10))));
            markByEvidenceId.put(eid, mlist);

            int entries = 1 + rnd.nextInt(3);
            for (int e = 0; e < entries; e++) {
                Double sim = rnd.nextBoolean() ? rnd.nextDouble() : Double.NaN;
                Long occ = rnd.nextBoolean() ? (long) rnd.nextInt(10) : null;
                candidates.add(new pt.estga.processing.models.CandidateEvidence(eid, occ, sim == null ? Double.NaN : sim));
            }
        }

        var r1 = aggregator.aggregate(candidates, markByEvidenceId, 20, 0);
        AggregationAssertions.assertValidAggregation(r1);

        // repeat with same seed/data to ensure deterministic output
        var r2 = aggregator.aggregate(candidates, markByEvidenceId, 20, 0);
        assertEquals(r1.topScores().size(), r2.topScores().size());
        for (int i = 0; i < r1.topScores().size(); i++) {
            assertEquals(r1.topScores().get(i).markId(), r2.topScores().get(i).markId());
            assertEquals(r1.topScores().get(i).confidence(), r2.topScores().get(i).confidence(), 1e-6);
        }
    }
}

