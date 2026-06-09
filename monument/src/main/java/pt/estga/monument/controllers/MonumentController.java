package pt.estga.monument.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.monument.mappers.MonumentMapper;
import pt.estga.monument.dtos.MonumentFilter;
import pt.estga.monument.services.MonumentService;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentListDto;
import pt.estga.monument.dtos.PolygonSearchRequest;

@RestController
@RequestMapping("/api/v1/public/monuments")
@RequiredArgsConstructor
@Tag(name = "Monuments", description = "Endpoints for monuments.")
public class MonumentController {

    private final MonumentService service;

    @GetMapping("/{id}")
    public ResponseEntity<MonumentDto> getMonumentById(
            @PathVariable Long id
    ) {
        return service.findById(id)
                .map(MonumentMapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<MonumentListDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) String name
    ) {
        MonumentFilter filter = new MonumentFilter(divisionId, name);
        return ResponseEntity.ok(service.search(filter, pageable).map(MonumentMapper::toListDto));
    }

    @PostMapping(value = "/search/polygon", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MonumentListDto>> searchMonumentsByPolygon(
            @Valid @RequestBody PolygonSearchRequest request,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByPolygon(request.geoJson(), pageable).map(MonumentMapper::toListDto));
    }

    @GetMapping("/division/{id}")
    public ResponseEntity<Page<MonumentListDto>> getMonumentsByDivision(
            @PathVariable Long id,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByDivisionId(id, pageable).map(MonumentMapper::toListDto));
    }
}
