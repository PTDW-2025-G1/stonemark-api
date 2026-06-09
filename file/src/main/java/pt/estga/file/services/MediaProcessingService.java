package pt.estga.file.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.repositories.MediaVariantRepository;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.storage.FileStorageService;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProcessingService {

    private static final Tika TIKA = new Tika();

    private final MediaFileRepository mediaFileRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final FileStorageService fileStorageService;
    private final ImageVariantGenerator imageVariantGenerator;
    private final FileNamingService fileNamingService;
    private final StorageProperties storageProperties;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void verifyWebpSupport() {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WEBP ImageIO writer not available. Check classpath.");
        }
    }

    @Transactional
    public void process(UUID mediaFileId, MediaVariantType... variantTypes) {
        List<MediaVariantType> types = List.of(variantTypes);
        if (types.isEmpty()) {
            log.debug("No variant types requested for media {} — skipping", mediaFileId);
            return;
        }
        log.info("Starting processing for media file ID: {} (types: {})", mediaFileId, types);
        var timer = Timer.start(meterRegistry);

        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("MediaFile not found: " + mediaFileId));

        try {
            mediaFile.setStatus(MediaStatus.PROCESSING);
            mediaFile = mediaFileRepository.save(mediaFile);

            Resource resource = fileStorageService.loadFile(mediaFile.getStoragePath());
            Path tempOriginal = createTempFile("original-", ".tmp");

            try {
                try (var is = resource.getInputStream()) {
                    Files.copy(is, tempOriginal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                if (!isAllowedImage(tempOriginal, Set.copyOf(storageProperties.getAllowedMimeTypes()))) {
                    log.warn("File {} is not a supported image, skipping variant generation.", mediaFile.getOriginalFilename());
                    mediaFile.setStatus(MediaStatus.READY);
                    mediaFile = mediaFileRepository.save(mediaFile);
                    return;
                }

                for (MediaVariantType type : types) {
                    if (mediaVariantRepository.existsByMediaFileAndType(mediaFile, type)) {
                        log.debug("Variant {} exists for media {}, skipping.", type, mediaFile.getId());
                        continue;
                    }

                    VariantResult generated = imageVariantGenerator.generate(tempOriginal, type);
                    try {
                        String prefixPath = fileNamingService.generatePath(mediaFile);
                        String variantPath = String.format("%s/derived/%s.webp", prefixPath, type.name().toLowerCase());
                        String storagePath;
                        try (java.io.InputStream is = Files.newInputStream(generated.file())) {
                            storagePath = fileStorageService.storeFile(is, variantPath, generated.size());
                        }

                        var variant = MediaVariant.builder()
                                .mediaFile(mediaFile)
                                .type(type)
                                .storagePath(storagePath)
                                .width(generated.width())
                                .height(generated.height())
                                .size(generated.size())
                                .build();

                        mediaVariantRepository.save(variant);
                        meterRegistry.counter("media.variant.generated").increment();
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Variant {} already persisted for media {} by concurrent processor — skipping",
                                type, mediaFile.getId());
                        meterRegistry.counter("media.variant.generated").increment();
                    } catch (Exception e) {
                        meterRegistry.counter("media.variant.failed").increment();
                        throw e;
                    } finally {
                        Files.deleteIfExists(generated.file());
                    }
                }

                mediaFile.setStatus(MediaStatus.READY);
                mediaFile = mediaFileRepository.save(mediaFile);
                log.info("Processing completed for media file ID: {}", mediaFileId);
            } finally {
                Files.deleteIfExists(tempOriginal);
            }

        } catch (Exception e) {
            log.error("Processing failed for media file ID: {}", mediaFileId, e);
            try {
                mediaFile.setStatus(MediaStatus.FAILED);
                mediaFileRepository.save(mediaFile);
            } catch (Exception ex) {
                log.error("Failed to mark media as FAILED for id {}", mediaFileId, ex);
            }
        } finally {
            timer.stop(meterRegistry.timer("media.processing.duration"));
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
}
