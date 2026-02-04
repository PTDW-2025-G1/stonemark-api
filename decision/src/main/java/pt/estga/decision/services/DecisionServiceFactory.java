package pt.estga.decision.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.repositories.ProposalRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecisionServiceFactory {

    private final List<ProposalDecisionService<?>> decisionServices;
    private final ProposalRepository<Proposal> proposalRepository;

    private Map<Class<?>, ProposalDecisionService<?>> serviceMap;

    private Map<Class<?>, ProposalDecisionService<?>> getServiceMap() {
        if (serviceMap == null) {
            serviceMap = decisionServices.stream()
                    .collect(Collectors.toMap(ProposalDecisionService::getProposalType, Function.identity()));
        }
        return serviceMap;
    }

    @SuppressWarnings("unchecked")
    public <T extends Proposal> ProposalDecisionService<T> getServiceForProposal(T proposal) {
        ProposalDecisionService<?> service = getServiceMap().get(proposal.getClass());
        if (service == null) {
            throw new IllegalArgumentException("No decision service found for proposal type: " + proposal.getClass().getSimpleName());
        }
        return (ProposalDecisionService<T>) service;
    }

    public ProposalDecisionService<?> getServiceForProposalId(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found with id: " + proposalId));
        return getServiceForProposal(proposal);
    }
}
