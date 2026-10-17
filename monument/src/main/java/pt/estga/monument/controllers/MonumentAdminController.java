package pt.estga.monument.controllers;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentRequestDto;
import pt.estga.monument.services.MonumentService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/monuments")
@RequiredArgsConstructor
@Tag(name = "Monuments Management", description = "Management endpoints for monuments.")
@PreAuthorize("hasRole('MODERATOR')")
public class MonumentAdminController {

    private final MonumentService service;

    @PostMapping
    public ResponseEntity<MonumentDto> createMonument(
            @Parameter(description = "Monument form data", required = true)
            @Valid @ModelAttribute MonumentRequestDto monumentDto
    ) {
        MonumentDto response = service.create(monumentDto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<MonumentDto> updateMonument(
            @PathVariable UUID id,
            @Parameter(description = "Monument form data", required = true)
            @Valid @ModelAttribute MonumentRequestDto monumentDto
    ) {
        return ResponseEntity.ok(service.update(id, monumentDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonument(
            @PathVariable UUID id
    ) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
