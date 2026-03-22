package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.events.MarkCreatedEvent;
import pt.estga.content.repositories.MarkRepository;
import pt.estga.file.entities.MediaFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkService {

    private final MarkRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<Mark> findById(Long id) {
        return repository.findById(id);
    }

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
            mark.setCover(cover);
        }
        Mark savedMark = repository.save(mark);
        if (savedMark.getCover() != null) {
            eventPublisher.publishEvent(new MarkCreatedEvent(this, savedMark.getId(), savedMark.getCover().getId(), savedMark.getCover().getOriginalFilename()));
        }
        return savedMark;
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
