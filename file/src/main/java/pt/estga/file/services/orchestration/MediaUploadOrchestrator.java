package pt.estga.file.services.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.metadata.MediaMetadataService;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

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

    @Transactional
    public MediaFile orchestrateUpload(InputStream input, String originalFilename, String contentType) throws IOException {
        // Generate a safe filename independent of DB id
        String storedFilename = fileNamingService.generateStoredFilename(originalFilename);

        StorageProvider provider = StorageProvider.valueOf(storageProperties.getProvider().toUpperCase());

        // Create metadata instance in memory (do not persist yet)
        MediaFile media = MediaFile.createForProcessing(storedFilename, originalFilename, provider);

        // Compute storage relative path using strategy
        String relativePath = storagePathStrategy.generatePath(media);

        // Store content
        var counting = new pt.estga.file.util.CountingInputStream(input);
        String storagePath = mediaContentService.saveContent(counting, relativePath);

        // Finalize entity state and persist once
        media.completeUpload(counting.getCount(), storagePath, null, MediaStatus.UPLOADED, Instant.now());
        MediaFile saved = mediaMetadataService.saveMetadata(media);

        // Publish event for async processing after commit
        eventPublisher.publishEvent(new MediaUploadedEvent(saved.getId()));

        return saved;
    }
}
