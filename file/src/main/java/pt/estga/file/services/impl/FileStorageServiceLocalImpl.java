package pt.estga.file.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import pt.estga.file.services.FileStorageService;
import pt.estga.sharedweb.exceptions.FileNotFoundException;
import pt.estga.sharedweb.exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class FileStorageServiceLocalImpl implements FileStorageService {

    private final Path rootPath;

    public FileStorageServiceLocalImpl(@Value("${storage.local.root-path:uploads}") String rootDir) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();

        try {
            log.info("Creating storage directory at: {}", rootPath);
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            log.error("Could not initialize storage directory", e);
            throw new FileStorageException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeFile(InputStream fileStream, String filename) {
        log.info("Storing file with filename: {}", filename);
        if (fileStream == null) {
            log.error("Cannot store empty file stream");
            throw new FileStorageException("Cannot store empty file stream");
        }

        try {
            // Resolve the full path
            Path targetPath = rootPath.resolve(filename).normalize();
            
            // Security check: ensure the path is inside the root directory
            if (!targetPath.startsWith(rootPath)) {
                log.error("Security check failed: Cannot store file outside of root path. Path: {}", filename);
                throw new SecurityException("Cannot store file outside of root path");
            }

            // Create parent directories if they don't exist
            Files.createDirectories(targetPath.getParent());

            log.debug("Writing file to: {}", targetPath);

            // Save file
            Files.copy(fileStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Return the relative path (which is the filename passed in)
            // Ensure we use forward slashes for consistency
            String relativePath = filename.replace("\\", "/");
            log.info("File stored successfully at: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new FileStorageException("Failed to store file", e);
        }
    }

    @Override
    public Resource loadFile(String path) {
        log.info("Loading file from path: {}", path);
        try {
            Path filePath = rootPath.resolve(path).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                log.error("File not found: {}", path);
                throw new FileNotFoundException("File not found: " + path);
            }

            log.info("File loaded successfully from path: {}", path);
            return resource;
        } catch (MalformedURLException e) {
            log.error("File not found: {}", path, e);
            throw new FileNotFoundException("File not found: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) {
        log.info("Deleting file from path: {}", path);
        try {
            Path filePath = rootPath.resolve(path).normalize();
            Files.deleteIfExists(filePath);
            
            // Try to delete the parent folder if it's empty (cleanup)
            Path parentDir = filePath.getParent();
            if (parentDir != null && !parentDir.equals(rootPath)) {
                try {
                    if (Files.list(parentDir).findAny().isEmpty()) {
                        Files.delete(parentDir);
                    }
                } catch (IOException ignored) {
                    // Ignore if folder not empty or other error
                }
            }

            log.info("File deleted successfully from path: {}", path);
        } catch (IOException e) {
            log.error("Could not delete file: {}", path, e);
            throw new FileStorageException("Could not delete file: " + path, e);
        }
    }
}
