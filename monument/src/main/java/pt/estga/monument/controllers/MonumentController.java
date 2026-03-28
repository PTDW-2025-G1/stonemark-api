package pt.estga.monument.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.monument.MonumentMapper;
import pt.estga.monument.services.MonumentQueryService;
import pt.estga.monument.dots.MonumentDto;
import pt.estga.monument.dots.MonumentListDto;
import pt.estga.sharedweb.models.PagedRequest;

@RestController
@RequestMapping("/api/v1/public/monuments")
@RequiredArgsConstructor
@Tag(name = "Monuments", description = "Endpoints for monuments.")
public class MonumentController {

    private final MonumentQueryService service;
    private final MonumentMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<MonumentDto> getMonumentById(
            @PathVariable Long id
    ) {
        return service.findById(id)
                .map(mapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/search")
    public ResponseEntity<Page<MonumentListDto>> searchMonuments(
            @RequestBody PagedRequest request
    ) {
        return ResponseEntity.ok(service.search(request));
    }

    @PostMapping(value = "/search/polygon", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MonumentListDto>> searchMonumentsByPolygon(
            @RequestBody String geoJson,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByPolygon(geoJson, pageable).map(mapper::toListDto));
    }

    @GetMapping("/division/{id}")
    public ResponseEntity<Page<MonumentListDto>> getMonumentsByDivision(
            @PathVariable Long id,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByDivisionId(id, pageable).map(mapper::toListDto));
    }
}
