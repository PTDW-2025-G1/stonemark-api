package pt.estga.mark.controllers;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.mark.dtos.PublicMarkDto;
import pt.estga.mark.dtos.PublicMarkSummaryDto;
import pt.estga.mark.services.MarkCatalogService;

@RestController
@RequestMapping("/api/v1/public/marks")
@RequiredArgsConstructor
@Tag(name = "Public Marks", description = "Public read-only access to published marks.")
public class PublicMarkController {

    private final MarkCatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<PublicMarkSummaryDto>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID monumentId,
            @RequestParam(required = false) Boolean located,
            @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return ResponseEntity.ok(catalogService.search(q, monumentId, located, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicMarkDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.findPublished(id));
    }
}
