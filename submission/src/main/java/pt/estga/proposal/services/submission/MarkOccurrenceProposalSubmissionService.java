package pt.estga.proposal.services.submission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.Monument;
import pt.estga.file.entities.MediaFile;
import pt.estga.proposal.dtos.MarkOccurrenceProposalCreateDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;
import pt.estga.user.entities.User;

@Service
@Slf4j
public class MarkOccurrenceProposalSubmissionService extends AbstractProposalSubmissionService<MarkOccurrenceProposal> {

    public MarkOccurrenceProposalSubmissionService(
            MarkOccurrenceProposalRepository repository,
            ApplicationEventPublisher eventPublisher) {
        super(repository, eventPublisher);
    }

    @Transactional
    public MarkOccurrenceProposal createAndSubmit(MarkOccurrenceProposalCreateDto dto, User user) {
        log.info("Creating and submitting new proposal for user: {}", user.getId());

        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .userNotes(dto.userNotes())
                .submissionSource(dto.submissionSource())
                .submittedBy(user)
                .newMark(true)
                .build();

        if (dto.photoId() != null) {
            proposal.setOriginalMediaFile(MediaFile.builder().id(dto.photoId()).build());
        }

        if (dto.existingMonumentId() != null) {
            proposal.setExistingMonument(Monument.builder().id(dto.existingMonumentId()).build());
        }

        if (dto.existingMarkId() != null) {
            proposal.setExistingMark(Mark.builder().id(dto.existingMarkId()).build());
            proposal.setNewMark(false);
        }

        return submit(proposal);
    }
}
