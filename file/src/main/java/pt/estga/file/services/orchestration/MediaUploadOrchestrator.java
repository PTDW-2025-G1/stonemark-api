package pt.estga.file.services.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
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

    @SuppressWarnings("unused")
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

        // Store content and obtain size info
        MediaContentService.SaveResult result = mediaContentService.saveContent(input, relativePath);

        // Finalize entity state and persist update
        media.completeUpload(result.size(), result.storagePath(), null, MediaStatus.UPLOADED);
        MediaFile saved = mediaMetadataService.saveMetadata(media);

        // Publish event for async processing after commit
        eventPublisher.publishEvent(new MediaUploadedEvent(saved.getId()));

        return saved;
    }
}
