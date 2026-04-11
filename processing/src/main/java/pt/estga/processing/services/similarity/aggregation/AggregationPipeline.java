package pt.estga.processing.services.similarity.aggregation;

import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.config.policies.ScoringPolicy;

import java.util.List;
import java.util.Map;

/**
 * AggregationPipeline orchestrates the scoring stages. It is independent from ScoreCalculator
 * to keep orchestration testable and composable.
 */
public class AggregationPipeline {

    private final CandidateNormalizationStage normalizationStage;
    private final CandidateDeduplicationStage deduplicationStage;
    private final FanOutResolver fanOutResolver;
    private final MarkScoringStage scoringStage;

    public AggregationPipeline(ScoringPolicy scoringPolicy) {
        this.normalizationStage = new CandidateNormalizationStage();
        this.deduplicationStage = new CandidateDeduplicationStage();
        this.fanOutResolver = new FanOutResolver();
        this.scoringStage = new MarkScoringStage(scoringPolicy);
    }

    public AggregationState execute(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        Map<Long, List<CandidateEvidence>> normalized = normalizationStage.normalize(contributionsByMark);

        // Compute fan-out based on the original input distribution (pre-normalization)
        // Fan-out must reflect how many marks an evidence originally appeared in
        // to avoid SPLIT scaling errors caused by later filtering.
        var fanOutCounts = fanOutResolver.computeFanOut(contributionsByMark);

        var dedupResult = deduplicationStage.deduplicate(normalized);
        var deduped = dedupResult.deduped();

        var scoringResult = scoringStage.score(deduped, fanOutCounts);

        return new AggregationState(scoringResult.scores(), scoringResult.weightSums(), dedupResult.duplicates(), scoringResult.perMarkContributions(), scoringResult.perMarkDecayApplied(), scoringResult.fanOutContributionCount(), scoringResult.weightAnomalies());
    }
}
