package pt.estga.mark.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkRequestDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.services.MarkQueryService;
import pt.estga.mark.services.MarkService;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/marks")
@RequiredArgsConstructor
@Tag(name = "Marks Moderation", description = "Moderation endpoints for marks.")
@PreAuthorize("hasRole('MODERATOR')")
public class MarkAdminController {

    private final MarkService service;
    private final MarkQueryService queryService;
    private final MarkMapper mapper;
    private final MediaService mediaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkDto> createMark(
            @RequestPart("data") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @Valid MarkRequestDto markDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Mark mark = mapper.toEntity(markDto);
        MediaFile mediaFile = resolveMediaFile(file, markDto.coverId());

        Mark createdMark = service.create(mark, mediaFile);
        MarkDto response = mapper.toDto(createdMark);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkDto> updateMark(
            @PathVariable Long id,
            @RequestPart("data") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @Valid MarkRequestDto markDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Mark existingMark = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark not found"));

        mapper.updateEntityFromDto(markDto, existingMark);
        MediaFile mediaFile = resolveMediaFile(file, markDto.coverId());

        Mark updatedMark = service.update(existingMark, mediaFile);
        return ResponseEntity.ok(mapper.toDto(updatedMark));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMark(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkDto> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Mark existingMark = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark not found"));

        MediaFile mediaFile = mediaService.save(file.getInputStream(), file.getOriginalFilename());
        Mark updatedMark = service.update(existingMark, mediaFile);

        return ResponseEntity.ok(mapper.toDto(updatedMark));
    }

    private MediaFile resolveMediaFile(MultipartFile file, Long coverId) throws IOException {
        if (file != null && !file.isEmpty()) {
            return mediaService.save(file.getInputStream(), file.getOriginalFilename());
        } else if (coverId != null) {
            return mediaService.findById(coverId).orElse(null);
        }
        return null;
    }
}
