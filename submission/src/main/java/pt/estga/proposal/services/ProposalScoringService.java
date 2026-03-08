package pt.estga.proposal.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.proposal.config.ProposalDecisionProperties;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.projections.ProposalStatsProjection;
import pt.estga.proposal.repositories.ProposalRepository;

@Service
@RequiredArgsConstructor
public class ProposalScoringService {

    private final ProposalDecisionProperties properties;
    private final ProposalRepository<Proposal> proposalRepository;

    public Integer calculatePriority(Proposal proposal) {
        int priority = 0;

        if (proposal instanceof MarkOccurrenceProposal markOccurrenceProposal) {
            // Start with credibility score as base
            priority += calculateCredibilityScore(proposal);

            // Boost for user reputation (based on previously approved proposals)
            if (proposal.getSubmittedBy() != null) {
                ProposalStatsProjection stats = proposalRepository.getStatsByUserId(proposal.getSubmittedBy().getId());
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

    public Integer calculateCredibilityScore(Proposal proposal) {
        int score = 0;

        // Base score for authenticated users
        if (proposal.getSubmittedBy() != null) {
            score += properties.getBaseScoreAuthenticatedUser();
        }

        // Completeness of data
        if (proposal.getUserNotes() != null && !proposal.getUserNotes().isEmpty()) {
            score += properties.getCompletenessScoreUserNotes();
        }

        if (proposal instanceof MarkOccurrenceProposal markOccurrenceProposal) {
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
