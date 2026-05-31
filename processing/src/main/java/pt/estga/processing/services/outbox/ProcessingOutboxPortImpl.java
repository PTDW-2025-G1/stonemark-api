package pt.estga.processing.services.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.entities.ProcessingOutbox;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.ProcessingOutboxRepository;
import pt.estga.shared.events.ProcessingOutboxPort;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProcessingOutboxPortImpl implements ProcessingOutboxPort {

    private final ProcessingOutboxRepository outboxRepository;

    @Override
    @Transactional
    public void enqueue(Long submissionId) {
        ProcessingOutbox outbox = ProcessingOutbox.builder()
                .submissionId(submissionId)
                .createdAt(Instant.now())
                .status(ProcessingStatus.PENDING)
                .build();
        outboxRepository.save(outbox);
    }
}
