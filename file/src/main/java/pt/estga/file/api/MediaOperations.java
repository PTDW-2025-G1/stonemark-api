package pt.estga.file.api;

import pt.estga.file.dtos.MediaFileDto;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module contract for the media catalogue and the submit staging flow.
 * Implemented by the file module; byte-level storage stays behind
 * {@code FileStorageService} (local filesystem today, S3 later).
 */
public interface MediaOperations {

    Optional<MediaFileDto> findById(UUID id);

    boolean existsById(UUID id);

    Optional<InputStream> openStream(UUID id);

    StagedFileRecord stage(InputStream data, String originalFilename);

    MediaFileDto commit(UUID stagingId, String originalFilename);

    void discardStaged(UUID stagingId, String originalFilename);
}
