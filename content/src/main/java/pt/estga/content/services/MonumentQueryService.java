package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.MonumentRepository;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.services.AdministrativeDivisionQueryService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentQueryService {

    private final MonumentRepository repository;

    public Page<Monument> findAll(Pageable pageable) {
        return repository.findByActive(pageable, true);
    }

    public Page<Monument> findAll(Pageable pageable, boolean active) {
        return repository.findByActive(pageable, active);
    }

    public Page<Monument> findAllWithDivisions(Pageable pageable, boolean active) {
        return repository.findAllWithDivisions(pageable, active);
    }

    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    public long count() {
        return repository.count();
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
