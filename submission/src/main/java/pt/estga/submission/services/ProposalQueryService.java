package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.submission.dtos.ProposalWithRelationsDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.mappers.MarkOccurrenceProposalMapper;
import pt.estga.submission.repositories.MarkOccurrenceProposalRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class ProposalQueryService {

    private final MarkOccurrenceProposalRepository proposalRepo;
    private final MarkOccurrenceProposalMapper proposalMapper;

    @Transactional(readOnly = true)
    public ProposalWithRelationsDto getProposalDetails(Long proposalId) {
        // Fetch proposal with eager relations (defined in repository)
        MarkOccurrenceSubmission proposal = proposalRepo.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + proposalId));

        return proposalMapper.toWithRelationsDto(proposal);
    }
}
