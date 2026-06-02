package pt.estga.territory.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import pt.estga.territory.services.DivisionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/divisions")
@RequiredArgsConstructor
@Tag(name = "Administrative Divisions", description = "Endpoints for administrative divisions.")
public class AdministrativeDivisionController {

    private final DivisionService service;

    @GetMapping("/roots")
    public ResponseEntity<List<AdministrativeDivisionDto>> getRoots() {
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(service.findRoots()));
    }

    @GetMapping
    public ResponseEntity<Page<AdministrativeDivisionDto>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministrativeDivisionDto> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(AdministrativeDivisionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<AdministrativeDivisionDto>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(service.findChildren(id)));
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
        return ResponseEntity.ok(AdministrativeDivisionMapper.toDtoList(service.findByCoordinates(latitude, longitude)));
    }
}
