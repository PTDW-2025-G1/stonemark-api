package pt.estga.file.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaProcessingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventListener {

    private final MediaProcessingService processingService;
    private final MediaMetadataService mediaMetadataService;

    @Async("fileTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMediaUploaded(MediaUploadedEvent event) {
        log.info("Received MediaUploadedEvent for media ID: {}", event.mediaFileId());
        try {
            processingService.process(event.mediaFileId());
        } catch (Exception e) {
            log.error("Unhandled failure processing media {} — marking FAILED", event.mediaFileId(), e);
            markMediaFailed(event.mediaFileId());
        }
    }

    private void markMediaFailed(java.util.UUID mediaFileId) {
        try {
            mediaMetadataService.findById(mediaFileId).ifPresent(media -> {
                if (media.getStatus() != MediaStatus.FAILED) {
                    media.setStatus(MediaStatus.FAILED);
                    mediaMetadataService.saveMetadata(media);
                }
            });
        } catch (Exception ex) {
            log.error("Failed to mark media {} as FAILED", mediaFileId, ex);
        }
    }
}
