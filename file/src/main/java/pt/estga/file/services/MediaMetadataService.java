package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.events.AfterCommitEventPublisher;
import org.springframework.context.ApplicationContext;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.enums.MediaStatus;

import java.time.Instant;
import java.util.List;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaMetadataService {

    private final MediaFileRepository mediaFileRepository;
    private final AfterCommitEventPublisher eventPublisher;
    private final ApplicationContext applicationContext;

    @Transactional
    public MediaFile saveMetadata(MediaFile mediaFile) {
        return mediaFileRepository.save(mediaFile);
    }

    /**
     * Saves metadata and registers the provided event to be published after the
     * surrounding transaction successfully commits. This method ensures the
     * save and event registration occur within the same transaction so handlers
     * will observe committed state.
     */
    @Transactional
    public MediaFile saveMetadataAndPublish(MediaFile mediaFile, Object event) {
        MediaFile saved = mediaFileRepository.save(mediaFile);
        eventPublisher.publish(event);
        return saved;
    }

    /**
     * Attempts to save metadata with a small number of retries. The final attempt
     * saves the entity and registers an event inside the transactional boundary so
     * the event is deferred until commit.
     */
    public MediaFile saveMetadataWithRetriesAndPublish(MediaFile mediaFile, Object event) {
        final int maxAttempts = 3;
        int tried = 0;
        while (true) {
            try {
                // Use Spring proxy to ensure the @Transactional on saveMetadataAndPublish
                // is applied. Calling the method directly would bypass the proxy and
                // would not start a new transaction for each attempt.
                MediaMetadataService self = applicationContext.getBean(MediaMetadataService.class);
                return self.saveMetadataAndPublish(mediaFile, event);
            } catch (Exception e) {
                tried++;
                if (tried >= maxAttempts) {
                    throw e;
                }
                log.warn("Transient error saving media metadata for id {} - retry {}/{}", mediaFile.getId(), tried, maxAttempts, e);
                try {
                    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(250L * tried);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying metadata save", ie);
                }
            }
        }
    }

    public Optional<MediaFile> findById(Long id) {
        return mediaFileRepository.findById(id);
    }

    public List<MediaFile> findProcessingOlderThan(Instant before) {
        return mediaFileRepository.findProcessingOlderThan(MediaStatus.PROCESSING, before);
    }
}
