package pt.estga.file.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;
import pt.estga.file.repositories.MediaVariantRepository;
import pt.estga.file.services.storage.FileStorageService;
import pt.estga.file.services.storage.variant.VariantStorageService;
import pt.estga.file.services.upload.MediaValidationService;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates media processing. Delegates validation, variant generation and storage
 * to dedicated services to keep orchestration logic compact and testable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProcessingService {

    private final MediaMetadataService mediaMetadataService;
    private final MediaVariantRepository mediaVariantRepository;
    private final FileStorageService fileStorageService;
    private final MediaValidationService mediaValidationService;
    private final ImageVariantGenerator imageVariantGenerator;
    private final VariantStorageService variantStorageService;
    private final StorageProperties storageProperties;
    private final TempFileFactory tempFileFactory;
    private final MediaMetricsService metrics;

    @PostConstruct
    public void verifyWebpSupport() {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WEBP ImageIO writer not available. Check classpath.");
        }
    }

    public void process(UUID mediaFileId) {
        log.info("Starting processing for media file ID: {}", mediaFileId);
        var timer = metrics.startProcessingTimer();

        MediaFile mediaFile = mediaMetadataService.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("MediaFile not found: " + mediaFileId));

        try {
            mediaFile.setStatus(MediaStatus.PROCESSING);
            mediaFile = mediaMetadataService.saveMetadata(mediaFile);

            Resource resource = fileStorageService.loadFile(mediaFile.getStoragePath());
            Path tempOriginal = tempFileFactory.createTempFile("original-", ".tmp");

            try {
                try (var is = resource.getInputStream()) {
                    Files.copy(is, tempOriginal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                if (!mediaValidationService.isAllowedImage(tempOriginal, Set.copyOf(storageProperties.getAllowedMimeTypes()))) {
                    log.warn("File {} is not a supported image, skipping variant generation.", mediaFile.getOriginalFilename());
                    mediaFile.setStatus(MediaStatus.READY);
                    mediaFile = mediaMetadataService.saveMetadata(mediaFile);
                    return;
                }

                List<MediaVariantType> variants = List.of(
                        MediaVariantType.THUMBNAIL,
                        MediaVariantType.PREVIEW,
                        MediaVariantType.OPTIMIZED
                );

                for (MediaVariantType type : variants) {
                    if (mediaVariantRepository.existsByMediaFileAndType(mediaFile, type)) {
                        log.debug("Variant {} exists for media {}, skipping.", type, mediaFile.getId());
                        continue;
                    }

                    VariantResult generated = imageVariantGenerator.generate(tempOriginal, type);
                    try {
                        String storagePath = variantStorageService.storeVariant(mediaFile, generated, type);

                        var variant = MediaVariant.builder()
                                .mediaFile(mediaFile)
                                .type(type)
                                .storagePath(storagePath)
                                .width(generated.width())
                                .height(generated.height())
                                .size(generated.size())
                                .build();

                        mediaVariantRepository.save(variant);
                        metrics.recordVariantGenerated();
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Variant {} already persisted for media {} by concurrent processor — skipping",
                                type, mediaFile.getId());
                        metrics.recordVariantGenerated();
                    } catch (Exception e) {
                        metrics.recordVariantFailed();
                        throw e;
                    } finally {
                        Files.deleteIfExists(generated.file());
                    }
                }

                mediaFile.setStatus(MediaStatus.READY);
                mediaFile = mediaMetadataService.saveMetadata(mediaFile);
                log.info("Processing completed for media file ID: {}", mediaFileId);
            } finally {
                Files.deleteIfExists(tempOriginal);
            }

        } catch (Exception e) {
            log.error("Processing failed for media file ID: {}", mediaFileId, e);
            try {
                mediaFile.setStatus(MediaStatus.FAILED);
                mediaMetadataService.saveMetadata(mediaFile);
            } catch (Exception ex) {
                log.error("Failed to mark media as FAILED for id {}", mediaFileId, ex);
            }
        } finally {
            metrics.recordProcessingDuration(timer);
        }
    }
}
