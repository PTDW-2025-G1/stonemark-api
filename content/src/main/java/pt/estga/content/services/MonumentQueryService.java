package pt.estga.content.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.content.entities.Monument;

import java.util.List;
import java.util.Optional;

public interface MonumentQueryService {

    Page<Monument> findAll(Pageable pageable);

    Page<Monument> findAll(Pageable pageable, boolean active);

    Page<Monument> findAllWithDivisions(Pageable pageable, boolean active);

    Optional<Monument> findById(Long id);

    long count();

    Page<Monument> searchByName(String query, Pageable pageable);

    Page<Monument> findByPolygon(String geoJson, Pageable pageable);

    Page<Monument> findByDivisionId(Long id, Pageable pageable);

}
