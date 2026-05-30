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
        String customDir = storageProperties.getTempDir();
        if (!StringUtils.hasText(customDir)) {
            return;
        }
        Path dir = Path.of(customDir);
        if (!Files.exists(dir)) {
            return;
        }
        Instant cutoff = Instant.now().minusSeconds(CLEANUP_AGE_MINUTES * 60);
        log.debug("Running temp file cleanup in {}", dir);
        try (Stream<Path> files = Files.list(dir)) {
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
            log.warn("Failed to list temp directory {}", dir, e);
        }
    }
}
