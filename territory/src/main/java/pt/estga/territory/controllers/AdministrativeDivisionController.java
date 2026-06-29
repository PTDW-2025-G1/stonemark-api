package pt.estga.territory.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.dtos.DivisionFilter;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;
import pt.estga.territory.services.DivisionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/divisions")
@RequiredArgsConstructor
@Tag(name = "Administrative Divisions", description = "Endpoints for administrative divisions.")
public class AdministrativeDivisionController {

    private final DivisionService service;
    private final AdministrativeDivisionRepository repository;

    @GetMapping("/roots")
    public ResponseEntity<List<AdministrativeDivisionDto>> getRoots() {
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(repository.findAllByParentIsNull()));
    }

    @GetMapping
    public ResponseEntity<Page<AdministrativeDivisionDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Boolean rootOnly) {
        DivisionFilter filter = new DivisionFilter(name, parentId, rootOnly);
        return ResponseEntity.ok(service.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministrativeDivisionDto> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(AdministrativeDivisionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<AdministrativeDivisionDto>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(repository.findByParentId(id)));
    }

    @GetMapping("/{id}/parent")
    public ResponseEntity<AdministrativeDivisionDto> getParent(@PathVariable Long id) {
        return service.findParent(id)
                .map(AdministrativeDivisionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/ancestors")
    public ResponseEntity<List<AdministrativeDivisionDto>> getAncestors(@PathVariable Long id) {
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(service.findAncestors(id)));
    }

    @GetMapping("/coordinates")
    public ResponseEntity<List<AdministrativeDivisionDto>> getDivisionsByCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(repository.findByCoordinates(latitude, longitude)));
    }

    @GetMapping("/coordinates/lowest")
    public ResponseEntity<AdministrativeDivisionDto> getLowestDivisionByCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return repository.findLowestContainingDivision(latitude, longitude)
                .map(AdministrativeDivisionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
