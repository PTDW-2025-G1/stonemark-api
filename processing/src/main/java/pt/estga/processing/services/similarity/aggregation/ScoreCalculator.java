package pt.estga.processing.services.similarity.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

@Component
public class ScoreCalculator {

    private final AggregationPipeline pipeline;

    public ScoreCalculator(ScoringPolicy scoringPolicy) {
        this.pipeline = new AggregationPipeline(scoringPolicy);
    }

    public AggregationState compute(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        return pipeline.execute(contributionsByMark);
    }
}
