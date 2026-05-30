package pt.estga.file.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;
import pt.estga.file.repositories.MediaVariantRepository;
import pt.estga.file.services.storage.variant.VariantStorageService;
import pt.estga.file.services.upload.MediaValidationService;

import org.springframework.util.StringUtils;
import java.io.IOException;
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
@Slf4j
public class MediaProcessingService {

    private final MediaMetadataService mediaMetadataService;
    private final MediaVariantRepository mediaVariantRepository;
    private final MediaContentService mediaContentService;
    private final MediaValidationService mediaValidationService;
    private final ImageVariantGenerator imageVariantGenerator;
    private final VariantStorageService variantStorageService;
    private final StorageProperties storageProperties;
    private final MediaMetricsService metrics;

    public MediaProcessingService(
            MediaMetadataService mediaMetadataService,
            MediaVariantRepository mediaVariantRepository,
            MediaContentService mediaContentService,
            MediaValidationService mediaValidationService,
            ImageVariantGenerator imageVariantGenerator,
            VariantStorageService variantStorageService,
            StorageProperties storageProperties,
            ObjectProvider<MediaMetricsService> metricsProvider) {
        this.mediaMetadataService = mediaMetadataService;
        this.mediaVariantRepository = mediaVariantRepository;
        this.mediaContentService = mediaContentService;
        this.mediaValidationService = mediaValidationService;
        this.imageVariantGenerator = imageVariantGenerator;
        this.variantStorageService = variantStorageService;
        this.storageProperties = storageProperties;
        this.metrics = metricsProvider.getIfAvailable();
    }

    @PostConstruct
    public void verifyWebpSupport() {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WEBP ImageIO writer not available. Check classpath.");
        }
    }

    public void process(UUID mediaFileId) {
        log.info("Starting processing for media file ID: {}", mediaFileId);
        var timer = metrics != null ? metrics.startProcessingTimer() : null;

        MediaFile mediaFile = mediaMetadataService.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("MediaFile not found: " + mediaFileId));

        try {
            mediaFile.setStatus(MediaStatus.PROCESSING);
            mediaFile = mediaMetadataService.saveMetadata(mediaFile);

            Resource resource = mediaContentService.loadContent(mediaFile.getStoragePath());
            Path tempOriginal = createTempFile("original-", ".tmp");

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
                        if (metrics != null) metrics.recordVariantGenerated();
                    } catch (Exception e) {
                        if (metrics != null) metrics.recordVariantFailed();
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
            if (timer != null) metrics.recordProcessingDuration(timer);
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
