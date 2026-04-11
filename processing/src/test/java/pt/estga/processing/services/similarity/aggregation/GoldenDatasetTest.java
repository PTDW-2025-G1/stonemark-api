package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.AggregationAssertions;
import pt.estga.processing.testutils.TestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GoldenDatasetTest {

    @Test
    void golden_dataset_produces_stable_topk_and_invariants() {
        CandidateGrouper grouper = new CandidateGrouper();
        ScoringPolicy scoringPolicy = new ScoringPolicy(true, 0.5, "SPLIT");
        ScoreCalculator calculator = new ScoreCalculator(scoringPolicy);
        AggregationResultBuilder builder = new AggregationResultBuilder();
        MarkAggregator aggregator = new MarkAggregator(grouper, calculator, builder);

        // Build 5 marks and 20 evidences with mixed duplicates / fanout / nulls
        Map<UUID, List<Mark>> markByEvidenceId = new LinkedHashMap<>();
        List<CandidateEvidence> candidates = new ArrayList<>();
        Random rnd = new Random(123);

        for (int i = 0; i < 20; i++) {
            UUID eid = UUID.nameUUIDFromBytes(("evid" + i).getBytes());
            int markCount = 1 + rnd.nextInt(4); // 1..4 marks
            List<Mark> marks = new ArrayList<>();
            for (int j = 0; j < markCount; j++) marks.add(TestBuilders.mark((long) (1 + rnd.nextInt(5))));
            markByEvidenceId.put(eid, marks);
            // Add 1-3 candidate entries per evidence with possible duplicates
            int entries = 1 + rnd.nextInt(3);
            for (int e = 0; e < entries; e++) {
                double sim = rnd.nextDouble();
                Long occ = rnd.nextBoolean() ? (long) rnd.nextInt(10) : null;
                candidates.add(new CandidateEvidence(eid, occ, sim));
            }
        }

        var result = aggregator.aggregate(Collections.unmodifiableList(candidates), markByEvidenceId, 10, 0);

        // Basic invariants
        AggregationAssertions.assertValidAggregation(result);

        // Top-K stability: repeated aggregate with same data must produce same top-K
        var result2 = aggregator.aggregate(Collections.unmodifiableList(candidates), markByEvidenceId, 10, 0);
        assertEquals(result.topScores().size(), result2.topScores().size());
        for (int i = 0; i < result.topScores().size(); i++) {
            assertEquals(result.topScores().get(i).markId(), result2.topScores().get(i).markId());
            assertEquals(result.topScores().get(i).confidence(), result2.topScores().get(i).confidence(), 1e-6);
        }
    }
}
