package pt.estga.proposal.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.proposal.dtos.ProposalWithRelationsDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.mappers.MarkOccurrenceProposalMapper;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class ProposalQueryService {

    private final MarkOccurrenceProposalRepository proposalRepo;
    private final MarkOccurrenceProposalMapper proposalMapper;

    @Transactional(readOnly = true)
    public ProposalWithRelationsDto getProposalDetails(Long proposalId) {
        // Fetch proposal with eager relations (defined in repository)
        MarkOccurrenceProposal proposal = proposalRepo.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found with id: " + proposalId));

        return proposalMapper.toWithRelationsDto(proposal);
    }
}
