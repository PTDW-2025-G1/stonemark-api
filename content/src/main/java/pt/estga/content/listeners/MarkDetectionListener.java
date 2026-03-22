package pt.estga.content.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.events.MarkCreatedEvent;
import pt.estga.content.events.MarkOccurrenceCreatedEvent;
import pt.estga.content.repositories.MarkOccurrenceRepository;
import pt.estga.content.repositories.MarkRepository;
import pt.estga.detection.model.DetectionResult;
import pt.estga.detection.service.DetectionService;
import pt.estga.file.services.MediaService;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarkDetectionListener {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository markOccurrenceRepository;
    private final MediaService mediaService;
    private final DetectionService detectionService;

    @Async
    @EventListener
    @Transactional
    public void handleMarkCreated(MarkCreatedEvent event) {
        try {
            var resource = mediaService.loadFileById(event.getCoverId());
            DetectionResult detectionResult = detectionService.detect(resource.getInputStream(), event.getFilename());

            if (detectionResult.isMasonMark()) {
                Optional<Mark> markOpt = markRepository.findById(event.getMarkId());
                if (markOpt.isPresent()) {
                    Mark mark = markOpt.get();
                    mark.setEmbedding(detectionResult.embedding());
                    markRepository.save(mark);
                }
            }
        } catch (IOException e) {
            log.error("Error processing mark detection for mark ID {}: {}", event.getMarkId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    @Transactional
    public void handleMarkOccurrenceCreated(MarkOccurrenceCreatedEvent event) {
        try {
            var resource = mediaService.loadFileById(event.getCoverId());
            DetectionResult detectionResult = detectionService.detect(resource.getInputStream(), event.getFilename());

            if (detectionResult.isMasonMark()) {
                Optional<MarkOccurrence> occurrenceOpt = markOccurrenceRepository.findById(event.getOccurrenceId());
                if (occurrenceOpt.isPresent()) {
                    MarkOccurrence occurrence = occurrenceOpt.get();
                    occurrence.setEmbedding(detectionResult.embedding());
                    markOccurrenceRepository.save(occurrence);
                }
            }
        } catch (IOException e) {
            log.error("Error processing mark occurrence detection for occurrence ID {}: {}", event.getOccurrenceId(), e.getMessage());
        }
    }
}
