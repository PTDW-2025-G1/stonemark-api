package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.Monument;
import pt.estga.file.entities.MediaFile;
import pt.estga.submission.dtos.MarkOccurrenceProposalCreateDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.events.SubmissionSubmittedEvent;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkOccurrenceSubmissionSubmitService {

    private final MarkOccurrenceSubmissionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MarkOccurrenceSubmission submit(MarkOccurrenceSubmission submission) {
        log.info("Submitting submission of type: {}", submission.getClass().getSimpleName());

        if (SubmissionStatus.SUBMITTED.equals(submission.getStatus())) {
            log.warn("Submission is already submitted. Skipping submission logic.");
            return submission;
        }

        submission.setSubmittedAt(Instant.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);

        MarkOccurrenceSubmission savedSubmission = repository.save(submission);
        log.info("Submission submitted successfully with ID: {}", savedSubmission.getId());

        eventPublisher.publishEvent(new SubmissionSubmittedEvent(this, savedSubmission.getId()));
        log.debug("Published SubmissionSubmittedEvent for submission ID: {}", savedSubmission.getId());

        return savedSubmission;
    }

    @Transactional
    public MarkOccurrenceSubmission createAndSubmit(MarkOccurrenceProposalCreateDto dto, User user) {
        log.info("Creating and submitting new proposal for user: {}", user.getId());

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
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
