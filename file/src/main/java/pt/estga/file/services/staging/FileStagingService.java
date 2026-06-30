package pt.estga.file.services.staging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.api.StagedFileRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStagingService {

    private final StorageProperties storageProperties;

    public StagedFileRecord stage(InputStream data, String originalFilename) {
        try {
            Path stagingDir = resolveStagingDir();
            Files.createDirectories(stagingDir);

            UUID id = UUID.randomUUID();
            String ext = extractExtension(originalFilename);
            Path target = stagingDir.resolve(id + ext);

            try (OutputStream out = Files.newOutputStream(target)) {
                data.transferTo(out);
            }

            long size = Files.size(target);
            log.debug("Staged file {} ({} bytes) as {}", originalFilename, size, target);
            return new StagedFileRecord(id, originalFilename, size);
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage file: " + originalFilename, e);
        }
    }

    public Path resolveStagedPath(UUID stagingId, String originalFilename) {
        Path stagingDir = resolveStagingDir();
        String ext = extractExtension(originalFilename);
        return stagingDir.resolve(stagingId + ext);
    }

    public void deleteStagedFile(UUID stagingId, String originalFilename) {
        try {
            Files.deleteIfExists(resolveStagedPath(stagingId, originalFilename));
        } catch (IOException e) {
            log.warn("Failed to delete staged file {}: {}", stagingId, e.getMessage());
        }
    }

    private Path resolveStagingDir() {
        String dir = storageProperties.getStagingDir();
        if (Path.of(dir).isAbsolute()) {
            return Path.of(dir);
        }
        String tempDir = storageProperties.getTempDir();
        if (StringUtils.hasText(tempDir)) {
            return Path.of(tempDir, dir);
        }
        return Path.of(System.getProperty("java.io.tmpdir"), dir);
    }

    private static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return ".bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".bin";
    }
}