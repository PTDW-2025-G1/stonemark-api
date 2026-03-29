package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkCommandService {

    private final MarkRepository repository;

    public Mark create(Mark mark) {
        return repository.save(mark);
    }

    public Mark update(Mark mark) {
        return repository.save(mark);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
