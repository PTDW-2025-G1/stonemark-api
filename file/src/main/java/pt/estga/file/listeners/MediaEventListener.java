package pt.estga.file.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.services.MediaProcessingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventListener {

    private final MediaProcessingService processingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMediaUploaded(MediaUploadedEvent event) {
        log.info("Received MediaUploadedEvent for media ID: {}", event.mediaFileId());
        processingService.process(event.mediaFileId());
    }
}
