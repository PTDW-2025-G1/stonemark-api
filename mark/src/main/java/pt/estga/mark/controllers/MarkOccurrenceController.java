package pt.estga.mark.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.services.MarkOccurrenceService;

@RestController
@RequestMapping("/api/v1/admin/occurrences")
@RequiredArgsConstructor
@Tag(name = "Mark Occurrences Management", description = "Management endpoints for mark occurrences.")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class MarkOccurrenceController {

    private final MarkOccurrenceService service;
    private final MarkOccurrenceRepository repository;

    @GetMapping
    public ResponseEntity<Page<MarkOccurrenceDto>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable).map(MarkOccurrenceMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarkOccurrenceDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
