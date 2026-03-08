package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.submission.config.SubmissionDecisionProperties;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;

@Service
@RequiredArgsConstructor
public class SubmissionScoringService {

    private final SubmissionDecisionProperties properties;
    private final MarkOccurrenceSubmissionRepository markOccurrenceSubmissionRepository;

    public Integer calculatePriority(MarkOccurrenceSubmission submission) {
        int priority = 0;

        // Start with credibility score as base
        priority += calculateCredibilityScore(submission);

        // Boost for user reputation (based on previously approved proposals)
        if (submission.getSubmittedBy() != null) {
            long approvedProposals = markOccurrenceSubmissionRepository.countAcceptedByUserId(submission.getSubmittedBy().getId());
            int reputationBoost = Math.min(
                (int) approvedProposals * properties.getReputationBoostPerApprovedProposal(),
                properties.getMaxReputationBoost()
            );
            priority += reputationBoost;
        }

        // Boost for new monument proposals (considered more valuable)
        if (submission.getExistingMonument() == null || submission.isNewMark()) {
            priority += properties.getNewMonumentProposalBoost();
        }

        return priority;
    }

    public Integer calculateCredibilityScore(MarkOccurrenceSubmission submission) {
        int score = 0;

        // Base score for authenticated users
        if (submission.getSubmittedBy() != null) {
            score += properties.getBaseScoreAuthenticatedUser();
        }

        // Completeness of data
        if (submission.getUserNotes() != null && !submission.getUserNotes().isEmpty()) {
            score += properties.getCompletenessScoreUserNotes();
        }

        if (submission.getLatitude() != null && submission.getLongitude() != null) {
            score += properties.getCompletenessScoreLocation();
        }

        if (submission.getOriginalMediaFile() != null) {
            score += properties.getCompletenessScoreMediaFile();
        }

        return Math.min(score, properties.getMaxCredibilityScore()); // Normalize to 0-100
    }
}
