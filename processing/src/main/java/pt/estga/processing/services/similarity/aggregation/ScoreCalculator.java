package pt.estga.processing.services.similarity.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

@Component
public class ScoreCalculator {

    private final CandidateNormalizationStage normalizationStage;
    private final CandidateDeduplicationStage deduplicationStage;
    private final FanOutResolver fanOutResolver;
    private final MarkScoringStage scoringStage;

    public ScoreCalculator(ScoringPolicy scoringPolicy) {
        this.normalizationStage = new CandidateNormalizationStage();
        this.deduplicationStage = new CandidateDeduplicationStage();
        this.fanOutResolver = new FanOutResolver();
        this.scoringStage = new MarkScoringStage(scoringPolicy);
    }

    public AggregationState compute(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        // Phase 1: Normalize
        Map<Long, List<CandidateEvidence>> normalized = normalizationStage.normalize(contributionsByMark);

        // Phase 2: Deduplicate
        CandidateDeduplicationStage.DeduplicationResult dedupResult = deduplicationStage.deduplicate(normalized);
        Map<Long, List<CandidateEvidence>> deduped = dedupResult.deduped();

        // Phase 3: Fan-out resolution (after dedup)
        Map<java.util.UUID, Integer> fanOutCounts = fanOutResolver.computeFanOut(deduped);

        // Phase 4: Scoring
        MarkScoringStage.ScoringResult scoringResult = scoringStage.score(deduped, fanOutCounts);

        // Build final aggregation state
        return new AggregationState(scoringResult.scores(), scoringResult.weightSums(), dedupResult.duplicates(), scoringResult.perMarkContributions(), scoringResult.perMarkDecayApplied(), scoringResult.fanOutContributionCount(), scoringResult.weightAnomalies());
    }
}
