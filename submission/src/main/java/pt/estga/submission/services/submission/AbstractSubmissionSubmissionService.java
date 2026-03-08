package pt.estga.submission.services.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.events.SubmissionSubmittedEvent;
import pt.estga.submission.repositories.SubmissionRepository;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractSubmissionSubmissionService<T extends Submission> {

    private final SubmissionRepository<T> repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public T submit(T proposal) {
        log.info("Submitting proposal of type: {}", proposal.getClass().getSimpleName());

        if (SubmissionStatus.SUBMITTED.equals(proposal.getStatus())) {
            log.warn("Submission is already submitted. Skipping submission logic.");
            return proposal;
        }

        proposal.setSubmittedAt(Instant.now());
        proposal.setStatus(SubmissionStatus.SUBMITTED);

        T savedProposal = repository.save(proposal);
        log.info("Submission submitted successfully with ID: {}", savedProposal.getId());

        eventPublisher.publishEvent(new SubmissionSubmittedEvent(this, savedProposal.getId()));
        log.debug("Published SubmissionSubmittedEvent for proposal ID: {}", savedProposal.getId());

        return savedProposal;
    }
}
