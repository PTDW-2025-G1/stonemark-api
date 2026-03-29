package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import pt.estga.shared.events.AfterCommitEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.events.MarkOccurrenceCreatedEvent;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.application.MediaService;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MarkOccurrenceService {

    private final MarkOccurrenceRepository repository;
    private final AfterCommitEventPublisher eventPublisher;
    private final MediaService mediaService;

    @Transactional
    public MarkOccurrence create(MarkOccurrence occurrence, MultipartFile file, Long coverId) throws IOException {
        return setImageAndSave(occurrence, file, coverId);
    }

    @Transactional
    public MarkOccurrence update(MarkOccurrence occurrence, MultipartFile file, Long coverId) throws IOException {
        return setImageAndSave(occurrence, file, coverId);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @NonNull
    private MarkOccurrence setImageAndSave(MarkOccurrence occurrence, MultipartFile file, Long coverId) throws IOException {
        MediaFile mediaFile = null;

        if (file != null && !file.isEmpty()) {
            mediaFile = mediaService.save(file.getInputStream(), file.getOriginalFilename());
        } else if (coverId != null) {
            mediaFile = mediaService.findById(coverId).orElse(null);
        }

        if (mediaFile != null) {
            occurrence.setCover(mediaFile);
        }

        MarkOccurrence savedOccurrence = repository.save(occurrence);
        if (savedOccurrence.getCover() != null) {
            eventPublisher.publish(new MarkOccurrenceCreatedEvent(this, savedOccurrence.getId(), savedOccurrence.getCover().getId(), savedOccurrence.getCover().getOriginalFilename()));
        }
        return savedOccurrence;
    }
}
