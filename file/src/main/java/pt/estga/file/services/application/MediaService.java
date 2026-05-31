package pt.estga.file.services.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.storage.FileStorageService;
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
    private final FileStorageService fileStorageService;

    public MediaFile upload(InputStream fileStream, String originalFilename, long fileSize) throws IOException {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return uploadOrchestrator.orchestrateUpload(fileStream, originalFilename);
    }

    public MediaType resolveMediaType(MediaFile mediaFile) {
        return MediaTypeFactory.getMediaType(mediaFile.getOriginalFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    public String buildDownloadFilename(MediaFile mediaFile) {
        String extension = StringUtils.getFilenameExtension(mediaFile.getOriginalFilename());
        return "stonemark-" + mediaFile.getId() + (extension != null ? "." + extension : "");
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
        return fileStorageService.loadFile(mediaFile.getStoragePath());
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
                fileStorageService.deleteFile(variant.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete variant file for {}: {}", variant.getType(), e.getMessage());
            }
        }

        if (StringUtils.hasText(mediaFile.getStoragePath())) {
            try {
                fileStorageService.deleteFile(mediaFile.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete main file: {}", e.getMessage());
            }
        }

        mediaMetadataService.deleteById(id);
    }
}
