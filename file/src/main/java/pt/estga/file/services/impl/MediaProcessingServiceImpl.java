package pt.estga.file.services.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.repositories.MediaVariantRepository;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaProcessingService;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.Files;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProcessingServiceImpl implements MediaProcessingService {

    private final MediaMetadataService mediaMetadataService;
    private final MediaVariantRepository mediaVariantRepository;
    private final MediaContentService mediaContentService;
    private final Tika tika = new Tika();

    @PostConstruct
    public void verifyWebpSupport() {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException(
                "WEBP ImageIO writer not available. Check classpath."
            );
        }
    }

    @Override
    public void process(Long mediaFileId) {
        log.info("Starting processing for media file ID: {}", mediaFileId);
        
        MediaFile mediaFile = mediaMetadataService.findById(mediaFileId)
                .orElseThrow(() -> new RuntimeException("MediaFile not found: " + mediaFileId));

        try {
            mediaFile.setStatus(MediaStatus.PROCESSING);
            mediaMetadataService.saveMetadata(mediaFile);

            // Load original file
            Resource resource = mediaContentService.loadContent(mediaFile.getStoragePath());
            File tempOriginalFile = Files.createTempFile("original-", mediaFile.getOriginalFilename()).toFile();
            
            try {
                try (InputStream is = resource.getInputStream();
                     OutputStream os = new FileOutputStream(tempOriginalFile)) {
                    is.transferTo(os);
                }

                // Security: Validate file type using Tika
                String detectedType = tika.detect(tempOriginalFile);
                log.info("Detected file type for {}: {}", mediaFile.getOriginalFilename(), detectedType);

                if (!isSupportedImage(detectedType)) {
                    log.warn("File {} is not a supported image (detected: {}), skipping variant generation.", mediaFile.getOriginalFilename(), detectedType);
                    // We might want to mark it as READY but without variants, or FAILED if strict.
                    // Assuming we keep the original but don't generate variants.
                    mediaFile.setStatus(MediaStatus.READY);
                    mediaMetadataService.saveMetadata(mediaFile);
                    return;
                }

                // Generate variants
                generateVariant(mediaFile, tempOriginalFile, MediaVariantType.THUMBNAIL);
                generateVariant(mediaFile, tempOriginalFile, MediaVariantType.PREVIEW);
                generateVariant(mediaFile, tempOriginalFile, MediaVariantType.OPTIMIZED);

                mediaFile.setStatus(MediaStatus.READY);
                mediaMetadataService.saveMetadata(mediaFile);
                log.info("Processing completed for media file ID: {}", mediaFileId);
            } finally {
                // Cleanup temp file
                if (tempOriginalFile.exists()) {
                    tempOriginalFile.delete();
                }
            }

        } catch (Exception e) {
            log.error("Processing failed for media file ID: {}", mediaFileId, e);
            mediaFile.setStatus(MediaStatus.FAILED);
            mediaMetadataService.saveMetadata(mediaFile);
        }
    }

    private void generateVariant(MediaFile mediaFile, File originalFile, MediaVariantType type) throws IOException {
        if (mediaVariantRepository.existsByMediaFileAndType(mediaFile, type)) {
            log.info("Variant {} already exists for media file ID: {}, skipping.", type, mediaFile.getId());
            return;
        }

        File tempVariantFile = Files.createTempFile("variant-", ".webp").toFile();
        
        try {
            Thumbnails.Builder<File> builder = Thumbnails.of(originalFile);
            
            // Configure based on type
            switch (type) {
                case THUMBNAIL:
                    builder.size(200, 200).crop(net.coobird.thumbnailator.geometry.Positions.CENTER);
                    break;
                case PREVIEW:
                    builder.size(1024, 1024);
                    break;
                case OPTIMIZED:
                    builder.scale(1.0); // Keep original size
                    break;
            }

            builder.outputFormat("webp").toFile(tempVariantFile);

            // Store variant
            // Note: We are using the same directory structure as the original file for variants
            // We can extract the directory from the original storage path or use a new strategy
            // For simplicity, let's put it in a 'derived' folder next to the original or use the ID
            String variantPath = String.format("%d/derived/%s.webp", mediaFile.getId(), type.name().toLowerCase());
            
            String storagePath;
            try (InputStream is = new FileInputStream(tempVariantFile)) {
                storagePath = mediaContentService.saveContent(is, variantPath);
            }

            // Get dimensions efficiently
            int width = 0;
            int height = 0;
            try (ImageInputStream in = ImageIO.createImageInputStream(tempVariantFile)) {
                final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        width = reader.getWidth(0);
                        height = reader.getHeight(0);
                    } finally {
                        reader.dispose();
                    }
                }
            }

            MediaVariant variant = MediaVariant.builder()
                    .mediaFile(mediaFile)
                    .type(type)
                    .storagePath(storagePath)
                    .width(width)
                    .height(height)
                    .size(tempVariantFile.length())
                    .build();
            
            mediaVariantRepository.save(variant);

        } finally {
            if (tempVariantFile.exists()) {
                tempVariantFile.delete();
            }
        }
    }

    private boolean isSupportedImage(String mimeType) {
        if (mimeType == null) return false;
        // Allow common image types
        return mimeType.equals("image/jpeg") || 
               mimeType.equals("image/png") || 
               mimeType.equals("image/webp");
    }
}
