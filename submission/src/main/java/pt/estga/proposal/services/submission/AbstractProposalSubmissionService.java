package pt.estga.proposal.services.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.events.ProposalSubmittedEvent;
import pt.estga.proposal.repositories.ProposalRepository;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractProposalSubmissionService<T extends Proposal> {

    private final ProposalRepository<T> repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public T submit(T proposal) {
        log.info("Submitting proposal of type: {}", proposal.getClass().getSimpleName());

        if (ProposalStatus.SUBMITTED.equals(proposal.getStatus())) {
            log.warn("Proposal is already submitted. Skipping submission logic.");
            return proposal;
        }

        proposal.setSubmittedAt(Instant.now());
        proposal.setStatus(ProposalStatus.SUBMITTED);

        T savedProposal = repository.save(proposal);
        log.info("Proposal submitted successfully with ID: {}", savedProposal.getId());

        eventPublisher.publishEvent(new ProposalSubmittedEvent(this, savedProposal.getId()));
        log.debug("Published ProposalSubmittedEvent for proposal ID: {}", savedProposal.getId());

        return savedProposal;
    }
}
