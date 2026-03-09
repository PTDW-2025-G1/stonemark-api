package pt.estga.content.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.content.dtos.MonumentDto;
import pt.estga.content.dtos.MonumentListDto;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.content.services.MonumentQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/monuments")
@RequiredArgsConstructor
@Tag(name = "Monuments", description = "Endpoints for monuments.")
public class MonumentController {

    private final MonumentQueryService service;
    private final MonumentMapper mapper;

    @GetMapping
    public ResponseEntity<Page<MonumentListDto>> getMonuments(
            @PageableDefault(size = 9) Pageable pageable
    ) {
        return ResponseEntity.ok(service.findAll(pageable).map(mapper::toListDto));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MonumentListDto>> searchMonuments(
            @RequestParam String query,
            @PageableDefault(size = 9, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.searchByName(query, pageable).map(mapper::toListDto));
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

    @GetMapping("/popular")
    public ResponseEntity<List<MonumentListDto>> getPopularMonuments(
            @RequestParam(defaultValue = "6") int limit
    ) {
        int safeLimit = Math.min(limit, 50);
        return ResponseEntity.ok(mapper.toListDto(service.findPopular(safeLimit)));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<MonumentListDto>> getLatestMonuments(
            @RequestParam(defaultValue = "6") int limit
    ) {
        int safeLimit = Math.min(limit, 50);
        return ResponseEntity.ok(mapper.toListDto(service.findLatest(safeLimit)));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countMonuments() {
        return ResponseEntity.ok(service.count());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonumentDto> getMonumentById(
            @PathVariable Long id
    ) {
        return service.findById(id)
                .map(mapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
