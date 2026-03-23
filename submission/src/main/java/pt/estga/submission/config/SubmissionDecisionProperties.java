package pt.estga.submission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties("proposal.decision")
public class SubmissionDecisionProperties {

    /**
     * Priority Scoring Configuration.
     * Parameters used to calculate the priority of a proposal, influencing the order of review.
     */
    private Integer reputationBoostPerApprovedProposal = 2;
    private Integer maxReputationBoost = 40;
    private Integer newMonumentProposalBoost = 5;

    /**
     * Credibility Scoring Configuration.
     * Parameters used to calculate the credibility score of a proposal based on user history,
     * data completeness, and content matching.
     */
    private Integer baseScoreAuthenticatedUser = 10;
    private Integer credibilityBoostPerApprovedProposal = 5;
    private Integer maxCredibilityBoostApprovedProposals = 50;
    private Integer completenessScoreLocation = 10;
    private Integer completenessScoreUserNotes = 5;
    private Integer completenessScoreMediaFile = 10;
    private Integer maxCredibilityScore = 100;

}
