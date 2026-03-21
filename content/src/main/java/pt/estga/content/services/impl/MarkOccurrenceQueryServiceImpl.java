package pt.estga.content.services.impl;

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
import pt.estga.content.services.MarkOccurrenceQueryService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkOccurrenceQueryServiceImpl implements MarkOccurrenceQueryService {

    private final MarkOccurrenceRepository repository;

    @Override
    public Page<MarkOccurrence> findAll(Pageable pageable) {
        return repository.findByActiveIsTrue(pageable);
    }

    @Override
    public Page<MarkOccurrence> findAllManagement(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<MarkOccurrence> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Page<MarkOccurrence> findByMarkId(Long markId, Pageable pageable) {
        return repository.findByMarkIdAndActiveIsTrue(markId, pageable);
    }

    @Override
    public List<MarkOccurrence> findByMarkIdForMap(Long markId) {
        return repository.findAllByMarkIdForMap(markId);
    }

    @Override
    public List<MarkOccurrence> findLatest(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findLatest(pageable);
    }

    @Override
    public Page<MarkOccurrence> findByMonumentId(Long monumentId, Pageable pageable) {
        return repository.findByMonumentIdAndActiveIsTrue(monumentId, pageable);
    }

    @Override
    public Page<MarkOccurrence> findByMarkIdAndMonumentId(Long markId, Long monumentId, Pageable pageable) {
        return repository.findByMarkIdAndMonumentIdAndActiveIsTrue(markId, monumentId, pageable);
    }

    @Override
    public List<Mark> findAvailableMarksByMonumentId(Long monumentId) {
        return repository.findDistinctMarksByMonumentId(monumentId);
    }

    @Override
    public List<Monument> findAvailableMonumentsByMarkId(Long markId) {
        return repository.findDistinctMonumentsByMarkId(markId);
    }

    @Override
    public long countByMonumentId(Long monumentId) {
        return repository.countByMonumentId(monumentId);
    }

    @Override
    public long countByMarkId(Long markId) {
        return repository.countByMarkId(markId);
    }

    @Override
    public long countDistinctMonumentsByMarkId(Long markId) {
        return repository.countDistinctMonumentIdByMarkId(markId);
    }
}
