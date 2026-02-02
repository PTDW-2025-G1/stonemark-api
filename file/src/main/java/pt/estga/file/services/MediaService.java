package pt.estga.file.services;

import org.springframework.core.io.Resource;
import pt.estga.file.entities.MediaFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Service interface for managing media files.
 * Handles both the metadata (database) and the physical storage of media content.
 */
public interface MediaService {

    /**
     * Saves a media file, persisting its metadata and storing its content.
     *
     * @param fileStream the input stream of the file content
     * @param filename   the original filename
     * @return the saved MediaFile entity containing metadata
     * @throws IOException if an I/O error occurs during storage
     */
    MediaFile save(InputStream fileStream, String filename) throws IOException;

    /**
     * Loads a file as a {@link Resource} from the given file id.
     *
     * @param fileId the id of the file to load
     * @return the loaded file as a {@link Resource}
     * @throws RuntimeException if the file cannot be loaded
     */
    Resource loadFileById(Long fileId);

    /**
     * Loads a file as a {@link Resource} from the given MediaFile entity.
     * Use this to avoid redundant DB lookups if the entity is already known.
     *
     * @param mediaFile the media file entity
     * @return the loaded file as a {@link Resource}
     */
    Resource loadFile(MediaFile mediaFile);

    /**
     * Finds a media file entity by its ID.
     *
     * @param id the id of the media file
     * @return an Optional containing the MediaFile if found
     */
    Optional<MediaFile> findById(Long id);

}
