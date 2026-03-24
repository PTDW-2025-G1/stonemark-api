package pt.estga.file.services.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.metadata.MediaMetadataService;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;

import java.io.InputStream;

/**
 * Handles the full upload orchestration in a linear, readable flow. Keeps
 * responsibilities separated: validation, naming, persistence, content storage
 * and event publication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaUploadOrchestrator {

    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;
    private final FileNamingService fileNamingService;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageProperties storageProperties;
    private final StoragePathStrategy storagePathStrategy;

    public MediaFile orchestrateUpload(InputStream input, String originalFilename) {
        // Generate a safe filename independent of DB id
        String storedFilename = fileNamingService.generateStoredFilename(originalFilename);

        StorageProvider provider = StorageProvider.valueOf(storageProperties.getProvider().toUpperCase());

        // Persist initial metadata so a record exists even if storage fails. This
        // allows retries/cleanup and avoids orphaned files without trace.
        MediaFile media = MediaFile.createForProcessing(storedFilename, originalFilename, provider);
        media = mediaMetadataService.saveMetadata(media);

        // Compute storage relative path using strategy (based on filename)
        String relativePath = storagePathStrategy.generatePath(media);

        // Store content and obtain size info. Handle storage failures by marking
        // the entity as FAILED so the system does not leave a PROCESSING record.
        MediaContentService.SaveResult result;
        try {
            result = mediaContentService.saveContent(input, relativePath);
        } catch (Exception e) {
            log.error("Storage failed for media id {}: {}", media.getId(), e.getMessage(), e);
            try {
                media.setStatus(MediaStatus.FAILED);
                mediaMetadataService.saveMetadata(media);
            } catch (Exception ex) {
                log.error("Failed to mark media as FAILED after storage error for id {}", media.getId(), ex);
            }
            throw e;
        }

        // Finalize entity state and persist update. Persist operation may fail
        // even though the file was stored; attempt a small retry before giving up
        // and marking the media as FAILED to avoid long-lived PROCESSING state.
        media.completeUpload(result.size(), result.storagePath(), null, MediaStatus.UPLOADED);
        MediaFile saved;
        try {
            saved = saveMetadataWithRetries(media, 3);
        } catch (Exception e) {
            log.error("CRITICAL: file stored but DB update failed for media id {}", media.getId(), e);
            try {
                media.setStatus(MediaStatus.FAILED);
                mediaMetadataService.saveMetadata(media);
            } catch (Exception ex) {
                log.error("Failed to mark media as FAILED after DB update error for id {}", media.getId(), ex);
            }
            throw new RuntimeException("Failed to persist final media state for id " + media.getId(), e);
        }

        // Ensure the processing event is only published after a successful commit.
        Runnable publishAction = () -> eventPublisher.publishEvent(new MediaUploadedEvent(saved.getId()));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
        } else {
            publishAction.run();
        }

        return saved;
    }

    /**
     * Attempts to save metadata with a small number of retries.
     * Retries are kept simple to avoid complex scheduling in the request
     * thread; the goal is to recover from transient DB errors.
     */
    private MediaFile saveMetadataWithRetries(MediaFile media, int attempts) {
        final int maxAttempts = attempts;
        int tried = 0;
        while (true) {
            try {
                return mediaMetadataService.saveMetadata(media);
            } catch (Exception e) {
                tried++;
                if (tried >= maxAttempts) {
                    throw e;
                }
                log.warn("Transient error saving media metadata for id {} - retry {}/{}", media.getId(), tried, maxAttempts, e);
                try {
                    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(250L * tried);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying metadata save", ie);
                }
            }
        }
    }
}
