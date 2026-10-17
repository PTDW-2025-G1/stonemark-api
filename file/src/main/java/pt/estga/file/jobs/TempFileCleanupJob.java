package pt.estga.file.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pt.estga.file.config.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempFileCleanupJob {

    private static final long CLEANUP_AGE_MINUTES = 30;

    private final StorageProperties storageProperties;

    @Scheduled(fixedDelayString = "PT30M")
    public void cleanupOrphanedTempFiles() {
        Instant cutoff = Instant.now().minusSeconds(CLEANUP_AGE_MINUTES * 60);

        // Only sweep a custom temp dir; never the shared system temp directory.
        String customDir = storageProperties.getTempDir();
        if (StringUtils.hasText(customDir)) {
            Path dir = Path.of(customDir);
            if (Files.isDirectory(dir)) {
                log.debug("Running temp file cleanup in {}", dir);
                cleanDirectory(dir, cutoff);
            }
        }

        Path stagingDir = resolveStagingDir();
        if (Files.isDirectory(stagingDir)) {
            log.debug("Running staging file cleanup in {}", stagingDir);
            cleanDirectory(stagingDir, cutoff);
        }
    }

    /**
     * Resolves the staging directory the same way {@code FileStagingService} does,
     * so abandoned staged uploads are cleaned even when no custom temp dir is set.
     */
    private Path resolveStagingDir() {
        String staging = storageProperties.getStagingDir();
        if (Path.of(staging).isAbsolute()) {
            return Path.of(staging);
        }
        String tempDir = storageProperties.getTempDir();
        if (StringUtils.hasText(tempDir)) {
            return Path.of(tempDir, staging);
        }
        return Path.of(System.getProperty("java.io.tmpdir"), staging);
    }

    private void cleanDirectory(Path directory, Instant cutoff) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(f -> {
                try {
                    return Files.isRegularFile(f)
                            && Files.getLastModifiedTime(f).toInstant().isBefore(cutoff);
                } catch (IOException e) {
                    log.warn("Failed to stat temp file {}", f, e);
                    return false;
                }
            }).forEach(f -> {
                try {
                    Files.deleteIfExists(f);
                    log.debug("Cleaned up orphaned temp file: {}", f);
                } catch (IOException e) {
                    log.warn("Failed to delete orphaned temp file {}", f, e);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to list directory {}", directory, e);
        }
    }
}
