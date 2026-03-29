package pt.estga.file.services.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Service interface for low-level file storage operations.
 * Implementations can provide storage on local file system, cloud storage (e.g., MinIO, S3), etc.
 */
public interface FileStorageService {

    /**
     * Stores a file from an input stream.
     *
     * @param fileStream the input stream of the file to store
     * @param filename   the name of the file to store (may include relative path)
     * @return the path or identifier where the file was stored
     * @throws RuntimeException if the file cannot be stored
     */
    String storeFile(InputStream fileStream, String filename);

    /**
     * Loads a file as a resource.
     *
     * @param path the path or identifier of the file to load
     * @return the file resource
     * @throws RuntimeException if the file cannot be found or loaded
     */
    Resource loadFile(String path);

    /**
     * Deletes a file from storage.
     *
     * @param path the path or identifier of the file to delete
     * @throws RuntimeException if the file cannot be deleted
     */
    void deleteFile(String path);
}
