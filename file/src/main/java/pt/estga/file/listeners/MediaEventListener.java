package pt.estga.file.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.events.MediaApprovedEvent;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.file.services.MediaProcessingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventListener {

    private final MediaProcessingService processingService;
    private final MediaFileRepository mediaFileRepository;

    @Async("fileTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMediaUploaded(MediaUploadedEvent event) {
        log.info("Media uploaded with ID: {} — generating optimized variant", event.mediaFileId());
        try {
            processingService.process(event.mediaFileId(), MediaVariantType.OPTIMIZED);
        } catch (Exception e) {
            log.error("Failed to generate optimized variant for media {} — marking FAILED", event.mediaFileId(), e);
            markMediaFailed(event.mediaFileId());
        }
    }

    @Async("fileTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMediaApproved(MediaApprovedEvent event) {
        log.info("Media approved with ID: {} — generating display variants", event.mediaFileId());
        try {
            processingService.process(event.mediaFileId(), MediaVariantType.THUMBNAIL, MediaVariantType.PREVIEW);
        } catch (Exception e) {
            log.error("Unhandled failure processing media {} — marking FAILED", event.mediaFileId(), e);
            markMediaFailed(event.mediaFileId());
        }
    }

    @Transactional
    private void markMediaFailed(java.util.UUID mediaFileId) {
        try {
            mediaFileRepository.findById(mediaFileId).ifPresent(media -> {
                if (media.getStatus() != MediaStatus.FAILED) {
                    media.setStatus(MediaStatus.FAILED);
                    mediaFileRepository.save(media);
                }
            });
        } catch (Exception ex) {
            log.error("Failed to mark media {} as FAILED", mediaFileId, ex);
        }
    }
}
