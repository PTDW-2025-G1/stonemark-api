package pt.estga.file.services.storage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.config.MinioProperties;
import pt.estga.sharedweb.exceptions.FileNotFoundException;
import pt.estga.sharedweb.exceptions.FileStorageException;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
@Slf4j
public class FileStorageServiceMinioImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public FileStorageServiceMinioImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.bucketName = minioProperties.getBucketName();
    }

    @Override
    public String storeFile(InputStream fileStream, String filename, long size) {
        log.debug("Storing file with filename: {} (size: {})", filename, size);
        if (fileStream == null) {
            log.error("Cannot store empty file stream");
            throw new FileStorageException("Cannot store empty file stream");
        }

        try {
            log.debug("Putting object with name: {}", filename);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(fileStream, size, -1L)
                            .build()
            );

            log.debug("File stored successfully with object name: {}", filename);
            return filename;
        } catch (MinioException e) {
            log.error("Failed to store file in MinIO", e);
            throw new FileStorageException("Failed to store file in MinIO", e);
        }
    }

    @Override
    public Resource loadFile(String path) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return new InputStreamResource(stream);
        } catch (ErrorResponseException e) {
            if (e.response() != null && e.response().code() == 404) {
                throw new FileNotFoundException("File not found in MinIO: " + path, e);
            }
            log.error("Failed to load file from MinIO with path: {}", path, e);
            throw new FileStorageException("Failed to load file from MinIO: " + path, e);
        } catch (MinioException e) {
            log.error("Failed to load file from MinIO with path: {}", path, e);
            throw new FileStorageException("Failed to load file from MinIO: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) {
        log.debug("Deleting file from path: {}", path);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            log.debug("File deleted successfully from path: {}", path);
        } catch (MinioException e) {
            log.error("Could not delete file from MinIO with path: {}", path, e);
            throw new FileStorageException("Could not delete file from MinIO", e);
        }
    }
}
