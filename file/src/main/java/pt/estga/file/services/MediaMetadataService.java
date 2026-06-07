package pt.estga.file.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.commoncore.events.AfterCommitEventPublisher;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.enums.MediaStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaMetadataService {

    private final MediaFileRepository mediaFileRepository;
    private final AfterCommitEventPublisher eventPublisher;
    private final TransactionTemplate requiresNewTemplate;

    public MediaMetadataService(MediaFileRepository mediaFileRepository,
                                AfterCommitEventPublisher eventPublisher,
                                PlatformTransactionManager ptm) {
        this.mediaFileRepository = mediaFileRepository;
        this.eventPublisher = eventPublisher;
        this.requiresNewTemplate = new TransactionTemplate(ptm);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public MediaFile saveMetadata(MediaFile mediaFile) {
        return mediaFileRepository.save(mediaFile);
    }

    /**
     * Saves metadata with retries for transient database failures only.
     * Non-transient errors (constraint violations, data integrity) fail immediately.
     * Each attempt runs in a new transaction (REQUIRES_NEW) to avoid pinning
     * the connection pool during backoff.
     */
    public MediaFile saveMetadataWithRetry(MediaFile mediaFile) {
        final int maxAttempts = 3;
        int tried = 0;
        while (true) {
            try {
                return requiresNewTemplate.execute(status -> mediaFileRepository.save(mediaFile));
            } catch (DataIntegrityViolationException e) {
                throw e;
            } catch (DataAccessException e) {
                tried++;
                if (tried >= maxAttempts) {
                    throw e;
                }
                log.warn("Transient error saving media metadata for id {} - retry {}/{}",
                        mediaFile.getId(), tried, maxAttempts, e);
                try {
                    long backoff = (long) (250L * tried * (0.5 + Math.random()));
                    TimeUnit.MILLISECONDS.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying metadata save", ie);
                }
            }
        }
    }

    public void publishAfterCommit(Object event) {
        eventPublisher.publish(event);
    }

    public Optional<MediaFile> findById(UUID id) {
        return mediaFileRepository.findById(id);
    }

    public List<MediaFile> findProcessingOlderThan(Instant before) {
        return mediaFileRepository.findProcessingOlderThan(MediaStatus.PROCESSING, before);
    }

    @Transactional
    public void deleteById(UUID id) {
        mediaFileRepository.deleteById(id);
    }
}
