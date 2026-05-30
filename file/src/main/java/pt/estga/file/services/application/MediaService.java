package pt.estga.file.services.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.upload.MediaUploadOrchestrator;
import pt.estga.sharedweb.exceptions.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaUploadOrchestrator uploadOrchestrator;
    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;

    public MediaFile save(InputStream fileStream, String originalFilename, long fileSize) throws IOException {
        return uploadOrchestrator.orchestrateUpload(fileStream, originalFilename, fileSize);
    }

    public Resource loadFileById(UUID fileId) {
        log.debug("Loading file with ID: {}", fileId);
        MediaFile mediaFile = mediaMetadataService.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("MediaFile not found with id: " + fileId));
        return loadFile(mediaFile);
    }

    public Resource loadFile(MediaFile mediaFile) {
        if (mediaFile.getStoragePath() == null || mediaFile.getStoragePath().isEmpty()) {
            throw new FileNotFoundException("Media file has no storage path");
        }
        return mediaContentService.loadContent(mediaFile.getStoragePath());
    }

    public Optional<MediaFile> findById(UUID id) {
        return mediaMetadataService.findById(id);
    }

    @Transactional
    public void deleteMedia(UUID id) {
        MediaFile mediaFile = mediaMetadataService.findById(id)
                .orElseThrow(() -> new FileNotFoundException("MediaFile not found with id: " + id));

        for (MediaVariant variant : List.copyOf(mediaFile.getVariants())) {
            try {
                mediaContentService.deleteContent(variant.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete variant file for {}: {}", variant.getType(), e.getMessage());
            }
        }

        if (StringUtils.hasText(mediaFile.getStoragePath())) {
            try {
                mediaContentService.deleteContent(mediaFile.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete main file: {}", e.getMessage());
            }
        }

        mediaMetadataService.deleteById(id);
    }
}
