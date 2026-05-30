package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.file.mappers.MediaFileMapper;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.services.upload.MediaUploadOrchestrator;
import pt.estga.fileapi.FileStorageOperations;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileStorageAdapter implements FileStorageOperations {

    private final MediaUploadOrchestrator orchestrator;
    private final MediaFileRepository repository;
    private final MediaFileMapper mediaFileMapper;

    @Override
    public MediaFileDto upload(InputStream data, String originalFilename) {
        try {
            var entity = orchestrator.orchestrateUpload(data, originalFilename);
            return mediaFileMapper.toDto(entity);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public Optional<MediaFileDto> findById(UUID id) {
        return repository.findById(id).map(mediaFileMapper::toDto);
    }
}
