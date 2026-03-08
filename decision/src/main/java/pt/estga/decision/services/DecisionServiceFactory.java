package pt.estga.decision.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.repositories.SubmissionRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecisionServiceFactory {

    private final List<SubmissionDecisionService<?>> decisionServices;
    private final SubmissionRepository<Submission> submissionRepository;

    private Map<Class<?>, SubmissionDecisionService<?>> serviceMap;

    private Map<Class<?>, SubmissionDecisionService<?>> getServiceMap() {
        if (serviceMap == null) {
            serviceMap = decisionServices.stream()
                    .collect(Collectors.toMap(SubmissionDecisionService::getProposalType, Function.identity()));
        }
        return serviceMap;
    }

    @SuppressWarnings("unchecked")
    public <T extends Submission> SubmissionDecisionService<T> getServiceForProposal(T proposal) {
        SubmissionDecisionService<?> service = getServiceMap().get(proposal.getClass());
        if (service == null) {
            throw new IllegalArgumentException("No decision service found for submission type: " + proposal.getClass().getSimpleName());
        }
        return (SubmissionDecisionService<T>) service;
    }

    public SubmissionDecisionService<?> getServiceForProposalId(Long proposalId) {
        Submission submission = submissionRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + proposalId));
        return getServiceForProposal(submission);
    }
}
