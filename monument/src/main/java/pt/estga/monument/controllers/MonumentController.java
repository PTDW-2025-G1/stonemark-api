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
import pt.estga.monument.repositories.MonumentRepository;
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
    private final MonumentRepository repository;
    private final MonumentMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<MonumentDto> getMonumentById(
            @PathVariable Long id
    ) {
        return repository.findById(id)
                .map(mapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<MonumentListDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String divisionCode,
            @RequestParam(required = false) String name
    ) {
        MonumentFilter filter = new MonumentFilter(divisionCode, name);
        return ResponseEntity.ok(service.search(filter, pageable).map(mapper::toListDto));
    }

    @PostMapping(value = "/search/polygon", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MonumentListDto>> searchMonumentsByPolygon(
            @Valid @RequestBody PolygonSearchRequest request,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(repository.findByPolygon(request.geoJson(), pageable).map(mapper::toListDto));
    }

    @GetMapping("/division/{code}")
    public ResponseEntity<Page<MonumentListDto>> getMonumentsByDivision(
            @PathVariable String code,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(repository.findByDivisionCode(code, pageable).map(mapper::toListDto));
    }
}
