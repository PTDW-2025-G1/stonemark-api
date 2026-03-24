package pt.estga.file.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.services.MediaMetadataService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Periodic job that finds media files stuck in PROCESSING state for longer than
 * a configured threshold and marks them as FAILED to prevent indefinite stalls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaRecoveryJob {

    private final MediaMetadataService mediaMetadataService;

    // Runs every 15 minutes
    @Scheduled(fixedDelayString = "PT15M")
    public void markStaleProcessingAsFailed() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<MediaFile> stuck = mediaMetadataService.findProcessingOlderThan(cutoff);
        if (stuck.isEmpty()) return;
        log.info("Found {} stuck processing media files older than {} - marking as FAILED", stuck.size(), cutoff);
        for (MediaFile m : stuck) {
            try {
                m.setStatus(MediaStatus.FAILED);
                mediaMetadataService.saveMetadata(m);
            } catch (Exception e) {
                log.error("Failed to mark media id {} as FAILED during recovery job", m.getId(), e);
            }
        }
    }
}
