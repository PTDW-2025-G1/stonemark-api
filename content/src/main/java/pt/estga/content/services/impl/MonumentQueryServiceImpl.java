package pt.estga.content.services.impl;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.MonumentQueryRepository;
import pt.estga.content.services.MonumentQueryService;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.services.AdministrativeDivisionQueryService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentQueryServiceImpl implements MonumentQueryService {

    private final MonumentQueryRepository repository;
    private final AdministrativeDivisionQueryService administrativeDivisionService;

    @Override
    public Page<Monument> findAll(Pageable pageable) {
        return repository.findByActive(pageable, true);
    }

    @Override
    public Page<Monument> findAll(Pageable pageable, boolean active) {
        return repository.findByActive(pageable, active);
    }

    @Override
    public Page<Monument> findAllWithDivisions(Pageable pageable, boolean active) {
        return repository.findAllWithDivisions(pageable, active);
    }

    @Override
    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Page<Monument> searchByName(String query, Pageable pageable) {
        return repository.findByNameContainingIgnoreCaseAndActive(query, pageable, true);
    }

    @Override
    public Page<Monument> findByPolygon(String geoJson, Pageable pageable) {
        return repository.findByPolygon(geoJson, pageable, true);
    }

    @Override
    public Page<Monument> findByDivisionId(Long id, Pageable pageable) {
        Optional<AdministrativeDivision> division = administrativeDivisionService.findById(id);
        if (division.isPresent()) {
            Geometry geometry = division.get().getGeometry();
            if (geometry != null) {
                return repository.findByGeometry(geometry, pageable, true);
            }
        }
        return Page.empty(pageable);
    }
}
