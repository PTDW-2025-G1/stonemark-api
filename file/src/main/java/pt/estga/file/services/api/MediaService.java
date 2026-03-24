package pt.estga.file.services.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.metadata.MediaMetadataService;
import pt.estga.file.services.orchestration.MediaUploadOrchestrator;
import pt.estga.sharedweb.exceptions.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaUploadOrchestrator uploadOrchestrator;
    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;

    /**
     * Deprecated: use MediaUploadOrchestrator.orchestrateUpload instead. Kept for
     * backward compatibility and delegates to the orchestrator.
     */
    @Deprecated
    @Transactional
    public MediaFile save(InputStream fileStream, String originalFilename) throws IOException {
        return uploadOrchestrator.orchestrateUpload(fileStream, originalFilename);
    }

    public Resource loadFileById(Long fileId) {
        log.info("Loading file with ID: {}", fileId);
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

    public Optional<MediaFile> findById(Long id) {
        return mediaMetadataService.findById(id);
    }
}
