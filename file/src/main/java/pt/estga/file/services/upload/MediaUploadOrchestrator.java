package pt.estga.file.services.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.exceptions.MediaPersistenceException;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaMetricsService;
import pt.estga.file.services.TempFileFactory;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.file.services.storage.FileStorageService;
import pt.estga.commonweb.exceptions.UnsupportedFileTypeException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaUploadOrchestrator {

    private final MediaMetadataService mediaMetadataService;
    private final FileStorageService fileStorageService;
    private final MediaValidationService mediaValidationService;
    private final FileNamingService fileNamingService;
    private final StorageProperties storageProperties;
    private final StoragePathStrategy storagePathStrategy;
    private final TempFileFactory tempFileFactory;
    private final MediaMetricsService metrics;

    public MediaFile orchestrateUpload(InputStream input, String originalFilename) throws IOException {
        long startNanos = System.nanoTime();
        metrics.recordUploadAttempt();

        Path tempFile = tempFileFactory.createTempFile("upload-", ".tmp");
        try {
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            long actualSize = Files.size(tempFile);

            if (actualSize > storageProperties.getMaxUploadSize()) {
                metrics.recordUploadRejected();
                throw new OversizeFileException(
                        "Uploaded file exceeds maximum allowed size of " + storageProperties.getMaxUploadSize() + " bytes");
            }

            Set<String> allowedTypes = Set.copyOf(storageProperties.getAllowedMimeTypes());
            if (!mediaValidationService.isAllowedImage(tempFile, allowedTypes)) {
                metrics.recordUploadRejected();
                throw new UnsupportedFileTypeException(
                        "File type is not allowed. Supported types: " + String.join(", ", allowedTypes));
            }

            String storedFilename = fileNamingService.generateStoredFilename(originalFilename);
            StorageProvider provider = StorageProvider.valueOf(storageProperties.getProvider().toUpperCase());

            String relativePath = storagePathStrategy.generatePath(storedFilename);

            String storagePath;
            try (InputStream fileIn = new FileInputStream(tempFile.toFile())) {
                storagePath = fileStorageService.storeFile(fileIn, relativePath, actualSize);
            } catch (Exception e) {
                log.error("Storage failed for file {}", storedFilename, e);
                throw e;
            }

            MediaFile media = MediaFile.createForProcessing(storedFilename, originalFilename, provider);
            media.completeUpload(actualSize, storagePath, null, MediaStatus.UPLOADED);

            try {
                MediaFile saved = mediaMetadataService.saveMetadataWithRetry(media);
                if (saved.getId() != null) {
                    var event = new MediaUploadedEvent(saved.getId());
                    mediaMetadataService.publishAfterCommit(event);
                }
                long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.recordUploadSuccess(actualSize, elapsed);
                return saved;
            } catch (Exception e) {
                try {
                    fileStorageService.deleteFile(storagePath);
                } catch (Exception ignored) {}
                metrics.recordUploadFailed();
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
}