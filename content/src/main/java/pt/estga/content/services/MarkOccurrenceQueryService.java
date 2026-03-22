package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.MarkOccurrenceRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkOccurrenceQueryService {

    private final MarkOccurrenceRepository repository;

    public Page<MarkOccurrence> findAll(Pageable pageable) {
        return repository.findByActiveIsTrue(pageable);
    }

    public Page<MarkOccurrence> findAllManagement(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<MarkOccurrence> findById(Long id) {
        return repository.findById(id);
    }

    public Page<MarkOccurrence> findByMarkId(Long markId, Pageable pageable) {
        return repository.findByMarkIdAndActiveIsTrue(markId, pageable);
    }

    public List<MarkOccurrence> findByMarkIdForMap(Long markId) {
        return repository.findAllByMarkIdForMap(markId);
    }

    public List<MarkOccurrence> findLatest(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findLatest(pageable);
    }

    public Page<MarkOccurrence> findByMonumentId(Long monumentId, Pageable pageable) {
        return repository.findByMonumentIdAndActiveIsTrue(monumentId, pageable);
    }

    public Page<MarkOccurrence> findByMarkIdAndMonumentId(Long markId, Long monumentId, Pageable pageable) {
        return repository.findByMarkIdAndMonumentIdAndActiveIsTrue(markId, monumentId, pageable);
    }

    public List<Mark> findAvailableMarksByMonumentId(Long monumentId) {
        return repository.findDistinctMarksByMonumentId(monumentId);
    }

    public List<Monument> findAvailableMonumentsByMarkId(Long markId) {
        return repository.findDistinctMonumentsByMarkId(markId);
    }

    public long countByMonumentId(Long monumentId) {
        return repository.countByMonumentId(monumentId);
    }

    public long countByMarkId(Long markId) {
        return repository.countByMarkId(markId);
    }

    public long countDistinctMonumentsByMarkId(Long markId) {
        return repository.countDistinctMonumentIdByMarkId(markId);
    }
}
