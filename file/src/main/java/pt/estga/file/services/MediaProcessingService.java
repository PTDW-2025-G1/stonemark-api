package pt.estga.file.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;
import pt.estga.file.repositories.MediaVariantRepository;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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
    private final MediaContentService mediaContentService;
    private final MediaValidationService mediaValidationService;
    private final VariantGeneratorService variantGeneratorService;
    private final VariantStorageService variantStorageService;

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");

    @PostConstruct
    public void verifyWebpSupport() {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WEBP ImageIO writer not available. Check classpath.");
        }
    }

    public void process(Long mediaFileId) {
        log.info("Starting processing for media file ID: {}", mediaFileId);

        MediaFile mediaFile = mediaMetadataService.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("MediaFile not found: " + mediaFileId));

        try {
            mediaFile.setStatus(MediaStatus.PROCESSING);
            mediaMetadataService.saveMetadata(mediaFile);

            Resource resource = mediaContentService.loadContent(mediaFile.getStoragePath());
            Path tempOriginal = Files.createTempFile("original-", ".tmp");

            try {
                try (var is = resource.getInputStream()) {
                    Files.copy(is, tempOriginal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                if (!mediaValidationService.isAllowedImage(tempOriginal, ALLOWED_MIME)) {
                    log.warn("File {} is not a supported image, skipping variant generation.", mediaFile.getOriginalFilename());
                    mediaFile.setStatus(MediaStatus.READY);
                    mediaMetadataService.saveMetadata(mediaFile);
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

                    VariantResult generated = variantGeneratorService.generate(tempOriginal, type);
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
                    } finally {
                        Files.deleteIfExists(generated.file());
                    }
                }

                mediaFile.setStatus(MediaStatus.READY);
                mediaMetadataService.saveMetadata(mediaFile);
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
        }
    }
}
