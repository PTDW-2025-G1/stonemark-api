package pt.estga.monument;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentQueryService {

    private final MonumentRepository repository;

    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    public Page<Monument> searchByName(String query, Pageable pageable) {
        return repository.findByNameContainingIgnoreCaseAndActive(query, pageable, true);
    }

    public Page<Monument> findByPolygon(String geoJson, Pageable pageable) {
        return repository.findByPolygon(geoJson, pageable, true);
    }

    public Page<Monument> findByDivisionId(Long id, Pageable pageable) {
        return repository.findByDivisionId(id, pageable, true);
    }
}
