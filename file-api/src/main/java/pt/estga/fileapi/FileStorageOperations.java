package pt.estga.fileapi;

import pt.estga.file.dtos.MediaFileDto;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public interface FileStorageOperations {

    MediaFileDto upload(InputStream data, String originalFilename);

    Optional<MediaFileDto> findById(UUID id);
}
