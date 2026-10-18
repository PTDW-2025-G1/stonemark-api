package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pt.estga.file.api.MediaOperations;
import pt.estga.file.api.StagedFileRecord;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.mappers.MediaFileMapper;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.services.staging.FileStagingService;
import pt.estga.file.services.storage.FileStorageService;
import pt.estga.file.services.upload.MediaUploadOrchestrator;
import pt.estga.shared.web.exceptions.FileNotFoundException;
import pt.estga.shared.web.exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The media catalogue: uploads, reads and deletes media, and implements the
 * cross-module {@link MediaOperations} contract. Bytes are stored through
 * {@link FileStorageService}, so the storage backend is swappable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService implements MediaOperations {

    private final MediaUploadOrchestrator uploadOrchestrator;
    private final MediaFileRepository mediaFileRepository;
    private final FileStorageService fileStorageService;
    private final FileStagingService stagingService;

    @Transactional
    public MediaFile upload(InputStream fileStream, String originalFilename, long fileSize) throws IOException {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return uploadOrchestrator.orchestrateUpload(fileStream, originalFilename);
    }

    @Override
    public Optional<MediaFileDto> findById(UUID id) {
        return mediaFileRepository.findById(id).map(MediaFileMapper::toDto);
    }

    @Override
    public boolean existsById(UUID id) {
        return mediaFileRepository.existsById(id);
    }

    @Override
    public Optional<InputStream> openStream(UUID id) {
        return mediaFileRepository.findById(id).map(mediaFile -> {
            try {
                return loadFile(mediaFile).getInputStream();
            } catch (IOException e) {
                throw new FileStorageException("Failed to open stream for file " + id, e);
            }
        });
    }

    @Override
    public StagedFileRecord stage(InputStream data, String originalFilename) {
        return stagingService.stage(data, originalFilename);
    }

    @Override
    @Transactional
    public MediaFileDto commit(UUID stagingId, String originalFilename) {
        try {
            Path stagedPath = stagingService.resolveStagedPath(stagingId, originalFilename);
            try (InputStream in = Files.newInputStream(stagedPath)) {
                MediaFile entity = uploadOrchestrator.orchestrateUpload(in, originalFilename);
                stagingService.deleteStagedFile(stagingId, originalFilename);
                return MediaFileMapper.toDto(entity);
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to commit staged file " + stagingId, e);
        }
    }

    @Override
    public void discardStaged(UUID stagingId, String originalFilename) {
        stagingService.deleteStagedFile(stagingId, originalFilename);
    }

    /** The media entity, for callers inside the file module that need more than the DTO. */
    public Optional<MediaFile> findEntityById(UUID id) {
        return mediaFileRepository.findById(id);
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
        MediaFile mediaFile = mediaFileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("MediaFile not found with id: " + fileId));
        return loadFile(mediaFile);
    }

    public Resource loadFile(MediaFile mediaFile) {
        if (mediaFile.getStoragePath() == null || mediaFile.getStoragePath().isEmpty()) {
            throw new FileNotFoundException("Media file has no storage path");
        }
        return fileStorageService.loadFile(mediaFile.getStoragePath());
    }

    @Transactional
    public void deleteMedia(UUID id) {
        MediaFile mediaFile = mediaFileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("MediaFile not found with id: " + id));

        List<String> variantPaths = List.copyOf(mediaFile.getVariants()).stream()
                .map(MediaVariant::getStoragePath)
                .toList();
        String mainPath = mediaFile.getStoragePath();

        mediaFileRepository.deleteById(id);

        for (String path : variantPaths) {
            try {
                fileStorageService.deleteFile(path);
            } catch (Exception e) {
                log.warn("Failed to delete variant file for media {}: {}", id, e.getMessage());
            }
        }
        if (StringUtils.hasText(mainPath)) {
            try {
                fileStorageService.deleteFile(mainPath);
            } catch (Exception e) {
                log.warn("Failed to delete main file for media {}: {}", id, e.getMessage());
            }
        }
    }
}
