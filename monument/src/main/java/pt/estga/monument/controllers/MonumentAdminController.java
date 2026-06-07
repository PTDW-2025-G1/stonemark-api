package pt.estga.monument.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.MonumentMapper;
import pt.estga.monument.services.MonumentService;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentRequestDto;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/monuments")
@RequiredArgsConstructor
@Tag(name = "Monuments Management", description = "Management endpoints for monuments.")
@PreAuthorize("hasAuthority('MONUMENT_WRITE')")
public class MonumentAdminController {

    private final MonumentService service;

    @PostMapping
    public ResponseEntity<MonumentDto> createMonument(
            @Parameter(description = "Monument form data", required = true)
            @Valid @ModelAttribute MonumentRequestDto monumentDto
    ) {
        Monument monument = MonumentMapper.toEntity(monumentDto);

        Monument createdMonument = service.save(monument);
        MonumentDto response = MonumentMapper.toResponseDto(createdMonument);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<MonumentDto> updateMonument(
            @PathVariable Long id,
            @Parameter(description = "Monument form data", required = true)
            @Valid @ModelAttribute MonumentRequestDto monumentDto
    ) {
        Monument existingMonument = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument not found"));

        MonumentMapper.updateEntityFromDto(monumentDto, existingMonument);

        Monument updatedMonument = service.save(existingMonument);
        return ResponseEntity.ok(MonumentMapper.toResponseDto(updatedMonument));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonument(
            @PathVariable Long id
    ) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
