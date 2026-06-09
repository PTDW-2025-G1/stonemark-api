package pt.estga.file.services.upload;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pt.estga.commoncore.events.AfterCommitEventPublisher;
import pt.estga.commonweb.exceptions.UnsupportedFileTypeException;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.exceptions.MediaPersistenceException;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.storage.FileStorageService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaUploadOrchestrator {

    private static final Tika TIKA = new Tika();

    private final MediaFileRepository mediaFileRepository;
    private final FileStorageService fileStorageService;
    private final FileNamingService fileNamingService;
    private final StorageProperties storageProperties;
    private final MeterRegistry meterRegistry;
    private final AfterCommitEventPublisher eventPublisher;
    private final PlatformTransactionManager ptm;

    private TransactionTemplate requiresNewTemplate;

    @PostConstruct
    void init() {
        this.requiresNewTemplate = new TransactionTemplate(ptm);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public MediaFile orchestrateUpload(InputStream input, String originalFilename) throws IOException {
        long startNanos = System.nanoTime();
        meterRegistry.counter("media.upload.total").increment();

        Path tempFile = createTempFile("upload-", ".tmp");
        try {
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            long actualSize = Files.size(tempFile);

            if (actualSize > storageProperties.getMaxUploadSize()) {
                meterRegistry.counter("media.upload.rejected").increment();
                throw new OversizeFileException(
                        "Uploaded file exceeds maximum allowed size of " + storageProperties.getMaxUploadSize() + " bytes");
            }

            Set<String> allowedTypes = Set.copyOf(storageProperties.getAllowedMimeTypes());
            if (!isAllowedImage(tempFile, allowedTypes)) {
                meterRegistry.counter("media.upload.rejected").increment();
                throw new UnsupportedFileTypeException(
                        "File type is not allowed. Supported types: " + String.join(", ", allowedTypes));
            }

            String storedFilename = fileNamingService.generateStoredFilename(originalFilename);
            StorageProvider provider = StorageProvider.valueOf(storageProperties.getProvider().toUpperCase());

            String relativePath = fileNamingService.generatePath(storedFilename);

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
                MediaFile saved = saveWithRetry(media);
                if (saved.getId() != null) {
                    eventPublisher.publish(new MediaUploadedEvent(saved.getId()));
                }
                long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                meterRegistry.counter("media.upload.success").increment();
                meterRegistry.summary("media.upload.size.bytes").record(actualSize);
                meterRegistry.timer("media.upload.duration").record(elapsed, TimeUnit.MILLISECONDS);
                return saved;
            } catch (Exception e) {
                try {
                    fileStorageService.deleteFile(storagePath);
                } catch (Exception ignored) {}
                meterRegistry.counter("media.upload.failed").increment();
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
        String dir = storageProperties.getTempDir();
        if (dir != null && !dir.isEmpty()) {
            Path dirPath = Path.of(dir);
            Files.createDirectories(dirPath);
            return Files.createTempFile(dirPath, prefix, suffix);
        }
        return Files.createTempFile(prefix, suffix);
    }

    private static boolean isAllowedImage(Path file, Set<String> allowedMimeTypes) throws IOException {
        String mime = TIKA.detect(file);
        log.debug("Detected MIME type {} for file {}", mime, file);
        return mime != null && allowedMimeTypes.contains(mime);
    }

    private MediaFile saveWithRetry(MediaFile mediaFile) {
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
}
