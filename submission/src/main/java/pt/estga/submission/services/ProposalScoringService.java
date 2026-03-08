package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.submission.config.SubmissionDecisionProperties;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.submission.repositories.ProposalRepository;

@Service
@RequiredArgsConstructor
public class ProposalScoringService {

    private final SubmissionDecisionProperties properties;
    private final ProposalRepository<Submission> proposalRepository;

    public Integer calculatePriority(Submission submission) {
        int priority = 0;

        if (submission instanceof MarkOccurrenceSubmission markOccurrenceProposal) {
            // Start with credibility score as base
            priority += calculateCredibilityScore(submission);

            // Boost for user reputation (based on previously approved proposals)
            if (submission.getSubmittedBy() != null) {
                ProposalStatsProjection stats = proposalRepository.getStatsByUserId(submission.getSubmittedBy().getId());
                int approvedProposals = stats != null ? (int) stats.getAccepted() : 0;
                int reputationBoost = Math.min(
                    approvedProposals * properties.getReputationBoostPerApprovedProposal(),
                    properties.getMaxReputationBoost()
                );
                priority += reputationBoost;
            }

            // Boost for new monument proposals (considered more valuable)
            if (markOccurrenceProposal.getExistingMonument() == null || markOccurrenceProposal.isNewMark()) {
                priority += properties.getNewMonumentProposalBoost();
            }
        }

        return priority;
    }

    public Integer calculateCredibilityScore(Submission submission) {
        int score = 0;

        // Base score for authenticated users
        if (submission.getSubmittedBy() != null) {
            score += properties.getBaseScoreAuthenticatedUser();
        }

        // Completeness of data
        if (submission.getUserNotes() != null && !submission.getUserNotes().isEmpty()) {
            score += properties.getCompletenessScoreUserNotes();
        }

        if (submission instanceof MarkOccurrenceSubmission markOccurrenceProposal) {
            if (markOccurrenceProposal.getLatitude() != null && markOccurrenceProposal.getLongitude() != null) {
                score += properties.getCompletenessScoreLocation();
            }
            if (markOccurrenceProposal.getOriginalMediaFile() != null) {
                score += properties.getCompletenessScoreMediaFile();
            }
        }

        return Math.min(score, properties.getMaxCredibilityScore()); // Normalize to 0-100
    }
}
