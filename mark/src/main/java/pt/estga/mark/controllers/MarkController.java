package pt.estga.mark.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.mark.services.MarkService;

@RestController
@RequestMapping("/api/v1/admin/marks")
@RequiredArgsConstructor
@Tag(name = "Marks Management", description = "Management endpoints for marks.")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class MarkController {

    private final MarkService service;
    private final MarkRepository repository;

    @GetMapping
    public ResponseEntity<Page<MarkDto>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable).map(MarkMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarkDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarkDto> update(@PathVariable Long id, @Valid @RequestBody MarkUpdateDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
