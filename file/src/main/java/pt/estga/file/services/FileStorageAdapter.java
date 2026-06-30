package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.file.mappers.MediaFileMapper;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.services.staging.FileStagingService;
import pt.estga.file.services.upload.MediaUploadOrchestrator;
import pt.estga.file.api.FileStorageOperations;
import pt.estga.file.api.StagedFileRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileStorageAdapter implements FileStorageOperations {

    private final MediaUploadOrchestrator orchestrator;
    private final MediaFileRepository repository;
    private final FileStagingService stagingService;
    private final MediaService mediaService;

    @Override
    @Transactional
    public MediaFileDto upload(InputStream data, String originalFilename) {
        try {
            var entity = orchestrator.orchestrateUpload(data, originalFilename);
            return MediaFileMapper.toDto(entity);
        } catch (java.io.IOException e) {
            throw new pt.estga.commonweb.exceptions.FileStorageException("Failed to upload file", e);
        }
    }

    @Override
    public StagedFileRecord stage(InputStream data, String originalFilename) {
        return stagingService.stage(data, originalFilename);
    }

    @Override
    @Transactional
    public MediaFileDto commit(UUID stagingId, String originalFilename) {
        try {
            var stagedPath = stagingService.resolveStagedPath(stagingId, originalFilename);
            try (var in = Files.newInputStream(stagedPath)) {
                var entity = orchestrator.orchestrateUpload(in, originalFilename);
                stagingService.deleteStagedFile(stagingId, originalFilename);
                return MediaFileMapper.toDto(entity);
            }
        } catch (java.io.IOException e) {
            throw new pt.estga.commonweb.exceptions.FileStorageException("Failed to commit staged file " + stagingId, e);
        }
    }

    @Override
    public Optional<MediaFileDto> findById(UUID id) {
        return repository.findById(id).map(MediaFileMapper::toDto);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public Optional<InputStream> openStream(UUID id) {
        return repository.findById(id).map(mediaFile -> {
            try {
                return mediaService.loadFile(mediaFile).getInputStream();
            } catch (IOException e) {
                throw new pt.estga.commonweb.exceptions.FileStorageException("Failed to open stream for file " + id, e);
            }
        });
    }
}
