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
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaMetricsService;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.file.services.storage.FileStorageService;
import pt.estga.file.util.CountingInputStream;
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
    private final FileStorageService fileStorageService;
    private final MediaValidationService mediaValidationService;
    private final FileNamingService fileNamingService;
    private final StorageProperties storageProperties;
    private final StoragePathStrategy storagePathStrategy;
    private final MediaMetricsService metrics;

    public MediaUploadOrchestrator(
            MediaMetadataService mediaMetadataService,
            FileStorageService fileStorageService,
            MediaValidationService mediaValidationService,
            FileNamingService fileNamingService,
            StorageProperties storageProperties,
            StoragePathStrategy storagePathStrategy,
            ObjectProvider<MediaMetricsService> metricsProvider) {
        this.mediaMetadataService = mediaMetadataService;
        this.fileStorageService = fileStorageService;
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

            String relativePath = storagePathStrategy.generatePath(storedFilename);

            SaveResult result;
            try (InputStream fileIn = new FileInputStream(tempFile.toFile())) {
                var counting = new CountingInputStream(fileIn);
                String storagePath = fileStorageService.storeFile(counting, relativePath);
                result = new SaveResult(storagePath, counting.getCount());
            } catch (Exception e) {
                log.error("Storage failed for file {}", storedFilename, e);
                throw e;
            }

            // Build complete entity after successful storage — no pre-set ID so Hibernate treats it as transient
            MediaFile media = MediaFile.createForProcessing(storedFilename, originalFilename, provider);
            media.setSize(actualSize);
            media.setStoragePath(result.storagePath());
            media.setStatus(MediaStatus.UPLOADED);

            try {
                MediaFile saved = mediaMetadataService.saveMetadataAndPublish(media);
                if (saved.getId() != null) {
                    var event = new MediaUploadedEvent(saved.getId());
                    mediaMetadataService.publishAfterCommit(event);
                }
                if (metrics != null) {
                    long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                    metrics.recordUploadSuccess(actualSize, elapsed);
                }
                return saved;
            } catch (Exception e) {
                try {
                    fileStorageService.deleteFile(result.storagePath());
                } catch (Exception ignored) {}
                if (metrics != null) metrics.recordUploadFailed();
                throw new MediaPersistenceException(
                        "Failed to persist media metadata for " + storedFilename, e);
            }
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
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