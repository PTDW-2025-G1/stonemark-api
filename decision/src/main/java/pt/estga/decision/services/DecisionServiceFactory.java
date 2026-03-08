package pt.estga.decision.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class DecisionServiceFactory {

    private final SubmissionDecisionService decisionService;
    private final MarkOccurrenceSubmissionRepository markOccurrenceSubmissionRepository;

    public SubmissionDecisionService getServiceForProposal(MarkOccurrenceSubmission proposal) {
        return decisionService;
    }

    public SubmissionDecisionService getServiceForProposalId(Long proposalId) {
        markOccurrenceSubmissionRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + proposalId));
        return decisionService;
    }
}
