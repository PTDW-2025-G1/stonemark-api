package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import pt.estga.shared.events.AfterCommitEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.events.MarkCreatedEvent;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.file.entities.MediaFile;

@Service
@RequiredArgsConstructor
public class MarkService {

    private final MarkRepository repository;
    private final AfterCommitEventPublisher eventPublisher;

    @Transactional
    public Mark create(Mark mark) {
        return create(mark, null);
    }

    @Transactional
    public Mark create(Mark mark, MediaFile cover) {
        return setPhotoAndSave(mark, cover);
    }

    @Transactional
    public Mark update(Mark mark) {
        return update(mark, null);
    }

    @Transactional
    public Mark update(Mark mark, MediaFile cover) {
        return setPhotoAndSave(mark, cover);
    }

    @NonNull
    private Mark setPhotoAndSave(Mark mark, MediaFile cover) {
        if (cover != null) {
            mark.setReferenceImage(cover);
        }
        Mark savedMark = repository.save(mark);
        if (savedMark.getReferenceImage() != null) {
            eventPublisher.publish(new MarkCreatedEvent(this, savedMark.getId(), savedMark.getReferenceImage().getId(), savedMark.getReferenceImage().getOriginalFilename()));
        }
        return savedMark;
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
