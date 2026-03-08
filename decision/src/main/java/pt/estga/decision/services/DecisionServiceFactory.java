package pt.estga.decision.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.repositories.ProposalRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecisionServiceFactory {

    private final List<ProposalDecisionService<?>> decisionServices;
    private final ProposalRepository<Submission> proposalRepository;

    private Map<Class<?>, ProposalDecisionService<?>> serviceMap;

    private Map<Class<?>, ProposalDecisionService<?>> getServiceMap() {
        if (serviceMap == null) {
            serviceMap = decisionServices.stream()
                    .collect(Collectors.toMap(ProposalDecisionService::getProposalType, Function.identity()));
        }
        return serviceMap;
    }

    @SuppressWarnings("unchecked")
    public <T extends Submission> ProposalDecisionService<T> getServiceForProposal(T proposal) {
        ProposalDecisionService<?> service = getServiceMap().get(proposal.getClass());
        if (service == null) {
            throw new IllegalArgumentException("No decision service found for submission type: " + proposal.getClass().getSimpleName());
        }
        return (ProposalDecisionService<T>) service;
    }

    public ProposalDecisionService<?> getServiceForProposalId(Long proposalId) {
        Submission submission = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + proposalId));
        return getServiceForProposal(submission);
    }
}
