package pt.estga.file.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.storage.FileStorageService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaRecoveryJob {

    private final MediaMetadataService mediaMetadataService;
    private final FileStorageService fileStorageService;

    @Scheduled(fixedDelayString = "PT15M")
    public void markStaleProcessingAsFailed() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<MediaFile> stuck = mediaMetadataService.findProcessingOlderThan(cutoff);
        if (stuck.isEmpty()) return;
        log.info("Found {} stuck processing media files older than {} - marking as FAILED", stuck.size(), cutoff);
        for (MediaFile m : stuck) {
            try {
                failWithCleanup(m);
            } catch (Exception e) {
                log.error("Failed to mark media id {} as FAILED during recovery job", m.getId(), e);
            }
        }
    }

    @Transactional
    protected void failWithCleanup(MediaFile mediaFile) {
        List<MediaVariant> variants = List.copyOf(mediaFile.getVariants());
        for (MediaVariant variant : variants) {
            try {
                fileStorageService.deleteFile(variant.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete variant storage for media {} variant {}: {}",
                        mediaFile.getId(), variant.getType(), e.getMessage());
            }
        }
        mediaFile.getVariants().clear();
        mediaFile.setStatus(MediaStatus.FAILED);
        mediaMetadataService.saveMetadata(mediaFile);
    }
}
