package pt.estga.territory.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import pt.estga.territory.services.DivisionService;

import java.util.List;
@RestController
@RequestMapping("/api/v1/public/divisions")
@RequiredArgsConstructor
@Tag(name = "Administrative Divisions", description = "Endpoints for administrative divisions.")
public class AdministrativeDivisionController {

    private final DivisionService service;
    private final AdministrativeDivisionMapper mapper;

    @PostMapping("/search")
    public ResponseEntity<Page<AdministrativeDivisionDto>> search(@RequestBody PagedRequest request) {
        return ResponseEntity.ok(service.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministrativeDivisionDto> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<AdministrativeDivisionDto>> getChildren(@PathVariable Long parentId) {
        List<AdministrativeDivision> children = service.findChildren(parentId);
        return ResponseEntity.ok(mapper.toDtoList(children));
    }

    @GetMapping("/{childId}/parent")
    public ResponseEntity<AdministrativeDivisionDto> getParent(@PathVariable Long childId) {
        return service.findParent(childId)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/coordinates")
    public ResponseEntity<List<AdministrativeDivisionDto>> getDivisionsByCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        List<AdministrativeDivision> divisions = service.findByCoordinates(latitude, longitude);
        return ResponseEntity.ok(mapper.toDtoList(divisions));
    }
}
