package pt.estga.file.services.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.dtos.SaveResult;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.exceptions.MediaPersistenceException;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaUploadOrchestrator {

    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;
    private final FileNamingService fileNamingService;
    private final StorageProperties storageProperties;
    private final StoragePathStrategy storagePathStrategy;

    public MediaFile orchestrateUpload(InputStream input, String originalFilename) throws IOException {
        String storedFilename = fileNamingService.generateStoredFilename(originalFilename);
        StorageProvider provider = StorageProvider.valueOf(storageProperties.getProvider().toUpperCase());

        MediaFile media = MediaFile.createForProcessing(storedFilename, originalFilename, provider);
        media = mediaMetadataService.saveMetadata(media);

        String relativePath = storagePathStrategy.generatePath(media);

        SaveResult result;
        try (InputStream in = input) {
            result = mediaContentService.saveContent(in, relativePath);
        } catch (Exception e) {
            markMediaFailed(media, "storage failed", e);
            throw e;
        }

        if (result.size() > storageProperties.getMaxUploadSize()) {
            try {
                mediaContentService.deleteContent(result.storagePath());
            } catch (Exception ignored) {}
            markMediaFailed(media, "uploaded file exceeds maximum allowed size", null);
            throw new OversizeFileException("Uploaded file exceeds maximum allowed size");
        }

        media.completeUpload(result.size(), result.storagePath(), null, MediaStatus.UPLOADED);

        try {
            return mediaMetadataService.saveMetadataWithRetriesAndPublish(
                    media, new MediaUploadedEvent(media.getId()));
        } catch (Exception e) {
            try {
                mediaContentService.deleteContent(result.storagePath());
            } catch (Exception ignored) {}
            markMediaFailed(media, "failed to persist final media state", e);
            throw new MediaPersistenceException(
                    "Failed to persist final media state for id " + media.getId(), e);
        }
    }

    private void markMediaFailed(MediaFile media, String reason, Exception cause) {
        try {
            if (cause != null) log.error("Marking media id {} as FAILED: {}", media.getId(), reason, cause);
            else log.error("Marking media id {} as FAILED: {}", media.getId(), reason);
            media.setStatus(MediaStatus.FAILED);
            mediaMetadataService.saveMetadata(media);
        } catch (Exception e) {
            log.error("Failed to mark media id {} as FAILED (reason: {})", media.getId(), reason, e);
        }
    }
}