package pt.estga.content.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.content.dtos.*;
import pt.estga.content.mappers.MarkMapper;
import pt.estga.content.mappers.MarkOccurrenceMapper;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.content.services.MarkOccurrenceQueryService;
import pt.estga.content.services.MarkSearchService;
import pt.estga.detection.model.DetectionResult;
import pt.estga.detection.service.DetectionService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/mark-occurrences")
@RequiredArgsConstructor
@Tag(name = "Mark Occurrences", description = "Endpoints for mark occurrences.")
public class MarkOccurrenceController {

    private final MarkOccurrenceQueryService service;
    private final MarkOccurrenceMapper mapper;
    private final DetectionService detectionService;
    private final MarkSearchService markSearchService;

    @GetMapping
    public ResponseEntity<Page<MarkOccurrenceDto>> getMarkOccurrences(
            @PageableDefault(size = 9) Pageable pageable
    ) {
        return ResponseEntity.ok(service.findAll(pageable).map(mapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarkOccurrenceDto> getMarkOccurrence(@PathVariable Long id) {
        return service.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/search/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> searchByImage(@RequestParam("file") MultipartFile file) throws IOException {
        DetectionResult detectionResult = detectionService.detect(file.getInputStream(), file.getOriginalFilename());

        if (!detectionResult.isMasonMark()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<String> similarOccurrenceIds = markSearchService.searchOccurrences(detectionResult.embedding());
        return ResponseEntity.ok(similarOccurrenceIds);
    }
}
