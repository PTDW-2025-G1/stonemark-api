package pt.estga.content.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.estga.content.dtos.MonumentDto;
import pt.estga.content.dtos.MonumentRequestDto;
import pt.estga.content.entities.Monument;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.content.services.MonumentQueryService;
import pt.estga.content.services.MonumentService;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.shared.exceptions.ResourceNotFoundException;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/monuments")
@RequiredArgsConstructor
@Tag(name = "Monuments Management", description = "Management endpoints for monuments.")
@PreAuthorize("hasRole('MODERATOR')")
public class AdminMonumentController {

    private final MonumentService service;
    private final MonumentQueryService queryService;
    private final MonumentMapper mapper;
    private final MediaService mediaService;

    @GetMapping()
    public ResponseEntity<Page<MonumentDto>> getMonumentsManagement(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false, defaultValue = "true") boolean active
    ) {
        return ResponseEntity.ok(queryService.findAllWithDivisions(pageable, active).map(mapper::toResponseDto));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MonumentDto> createMonument(
            @RequestPart("data") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @Valid MonumentRequestDto monumentDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Monument monument = mapper.toEntity(monumentDto);

        resolveAndSetMedia(monument, file, monumentDto.coverId());

        Monument createdMonument = service.create(monument);
        MonumentDto response = mapper.toResponseDto(createdMonument);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MonumentDto> updateMonument(
            @PathVariable Long id,
            @RequestPart("data") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @Valid MonumentRequestDto monumentDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Monument existingMonument = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument not found"));

        mapper.updateEntityFromDto(monumentDto, existingMonument);

        resolveAndSetMedia(existingMonument, file, monumentDto.coverId());

        Monument updatedMonument = service.update(existingMonument);
        return ResponseEntity.ok(mapper.toResponseDto(updatedMonument));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonument(
            @PathVariable Long id
    ) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MonumentDto> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Monument monument = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument not found"));

        MediaFile mediaFile = mediaService.save(file.getInputStream(), file.getOriginalFilename());
        monument.setCover(mediaFile);
        Monument updatedMonument = service.update(monument);

        return ResponseEntity.ok(mapper.toResponseDto(updatedMonument));
    }

    private void resolveAndSetMedia(Monument monument, MultipartFile file, Long coverId) throws IOException {
        if (file != null && !file.isEmpty()) {
            MediaFile mediaFile = mediaService.save(file.getInputStream(), file.getOriginalFilename());
            monument.setCover(mediaFile);
        } else if (coverId != null) {
            mediaService.findById(coverId).ifPresent(monument::setCover);
        }
    }
}
