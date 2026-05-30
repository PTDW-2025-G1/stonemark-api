package pt.estga.file.services.upload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.dtos.SaveResult;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.exceptions.MediaPersistenceException;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaMetricsService;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.sharedweb.exceptions.UnsupportedFileTypeException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class MediaUploadOrchestrator {

    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;
    private final MediaValidationService mediaValidationService;
    private final FileNamingService fileNamingService;
    private final StorageProperties storageProperties;
    private final StoragePathStrategy storagePathStrategy;
    private final MediaMetricsService metrics;

    public MediaUploadOrchestrator(
            MediaMetadataService mediaMetadataService,
            MediaContentService mediaContentService,
            MediaValidationService mediaValidationService,
            FileNamingService fileNamingService,
            StorageProperties storageProperties,
            StoragePathStrategy storagePathStrategy,
            ObjectProvider<MediaMetricsService> metricsProvider) {
        this.mediaMetadataService = mediaMetadataService;
        this.mediaContentService = mediaContentService;
        this.mediaValidationService = mediaValidationService;
        this.fileNamingService = fileNamingService;
        this.storageProperties = storageProperties;
        this.storagePathStrategy = storagePathStrategy;
        this.metrics = metricsProvider.getIfAvailable();
    }

    public MediaFile orchestrateUpload(InputStream input, String originalFilename) throws IOException {
        return orchestrateUpload(input, originalFilename, -1);
    }

    public MediaFile orchestrateUpload(InputStream input, String originalFilename, long fileSize) throws IOException {
        long startNanos = System.nanoTime();
        if (metrics != null) metrics.recordUploadAttempt();

        Path tempFile = createTempFile("upload-", ".tmp");
        try {
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            long actualSize = Files.size(tempFile);

            if (actualSize > storageProperties.getMaxUploadSize()) {
                if (metrics != null) metrics.recordUploadRejected();
                throw new OversizeFileException(
                        "Uploaded file exceeds maximum allowed size of " + storageProperties.getMaxUploadSize() + " bytes");
            }

            Set<String> allowedTypes = Set.copyOf(storageProperties.getAllowedMimeTypes());
            if (!mediaValidationService.isAllowedImage(tempFile, allowedTypes)) {
                if (metrics != null) metrics.recordUploadRejected();
                throw new UnsupportedFileTypeException(
                        "File type is not allowed. Supported types: " + String.join(", ", allowedTypes));
            }

            String storedFilename = fileNamingService.generateStoredFilename(originalFilename);
            StorageProvider provider = StorageProvider.valueOf(storageProperties.getProvider().toUpperCase());

            MediaFile media = MediaFile.createForProcessing(storedFilename, originalFilename, provider);
            media = mediaMetadataService.saveMetadata(media);

            String relativePath = storagePathStrategy.generatePath(media);

            SaveResult result;
            try (InputStream fileIn = new FileInputStream(tempFile.toFile())) {
                result = mediaContentService.saveContent(fileIn, relativePath);
            } catch (Exception e) {
                markMediaFailed(media, "storage failed", e);
                throw e;
            }

            media.completeUpload(actualSize, result.storagePath(), null, MediaStatus.UPLOADED);

            try {
                MediaFile saved = mediaMetadataService.saveMetadataAndPublish(
                        media, new MediaUploadedEvent(media.getId()));
                if (metrics != null) {
                    long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                    metrics.recordUploadSuccess(actualSize, elapsed);
                }
                return saved;
            } catch (Exception e) {
                try {
                    mediaContentService.deleteContent(result.storagePath());
                } catch (Exception ignored) {}
                markMediaFailed(media, "failed to persist final media state", e);
                if (metrics != null) metrics.recordUploadFailed();
                throw new MediaPersistenceException(
                        "Failed to persist final media state for id " + media.getId(), e);
            }
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
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

    private Path createTempFile(String prefix, String suffix) throws IOException {
        String customDir = storageProperties.getTempDir();
        if (StringUtils.hasText(customDir)) {
            Path dir = Path.of(customDir);
            Files.createDirectories(dir);
            return Files.createTempFile(dir, prefix, suffix);
        }
        return Files.createTempFile(prefix, suffix);
    }
}