package pt.estga.mark.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.services.MarkEvidenceService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/evidences")
@RequiredArgsConstructor
@Tag(name = "Mark Evidences Management", description = "Management endpoints for mark evidences.")
public class MarkEvidenceController {

    private final MarkEvidenceService service;

    @GetMapping
    public ResponseEntity<Page<MarkEvidenceDto>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarkEvidenceDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
