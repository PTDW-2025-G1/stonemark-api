package pt.estga.file.services.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.shared.events.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.naming.FileNamingService;
import pt.estga.file.naming.StoragePathStrategy;

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
    private final AfterCommitEventPublisher eventPublisher;
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

        // Enforce configured maximum upload size. If the stored file exceeds the
        // configured limit, delete it and fail the upload to keep system consistent.
        if (result.size() > storageProperties.getMaxUploadSize()) {
            try {
                mediaContentService.deleteContent(result.storagePath());
            } catch (Exception ex) {
                log.error("Failed to delete oversize uploaded content at {}", result.storagePath(), ex);
            }
            media.setStatus(MediaStatus.FAILED);
            mediaMetadataService.saveMetadata(media);
            throw new RuntimeException("Uploaded file exceeds maximum allowed size");
        }

        // Finalize entity state and persist update. Persist operation may fail
        // even though the file was stored; attempt a small retry before giving up
        // and marking the media as FAILED to avoid long-lived PROCESSING state.
        media.completeUpload(result.size(), result.storagePath(), null, MediaStatus.UPLOADED);
        MediaFile saved;
        try {
            saved = saveMetadataWithRetries(media);
        } catch (Exception e) {
            log.error("CRITICAL: file stored but DB update failed for media id {}", media.getId(), e);
            // Attempt to delete stored content to avoid orphaned files. If deletion
            // fails, log and leave for manual/periodic cleanup.
            try {
                String path = result.storagePath();
                mediaContentService.deleteContent(path);
                log.info("Deleted stored content at {} after DB update failure for media id {}", path, media.getId());
            } catch (Exception ex) {
                log.error("Failed to delete stored content after DB update failure for media id {}", media.getId(), ex);
            }

            try {
                media.setStatus(MediaStatus.FAILED);
                mediaMetadataService.saveMetadata(media);
            } catch (Exception ex) {
                log.error("Failed to mark media as FAILED after DB update error for id {}", media.getId(), ex);
            }
            throw new RuntimeException("Failed to persist final media state for id " + media.getId(), e);
        }

        // Publish the uploaded event after commit using the centralized abstraction.
        eventPublisher.publish(new MediaUploadedEvent(saved.getId()));

        return saved;
    }

    /**
     * Attempts to save metadata with a small number of retries.
     * Retries are kept simple to avoid complex scheduling in the request
     * thread; the goal is to recover from transient DB errors.
     */
    private MediaFile saveMetadataWithRetries(MediaFile media) {
        final int maxAttempts = 3;
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
