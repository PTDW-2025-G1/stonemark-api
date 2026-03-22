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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.estga.content.dtos.MarkOccurrenceDto;
import pt.estga.content.dtos.MarkOccurrenceRequestDto;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.mappers.MarkOccurrenceMapper;
import pt.estga.content.services.MarkOccurrenceQueryService;
import pt.estga.content.services.MarkOccurrenceService;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/mark-occurrences")
@RequiredArgsConstructor
@Tag(name = "Mark Occurrences Management", description = "Moderation endpoints for mark occurrences.")
@PreAuthorize("hasRole('MODERATOR')")
public class AdminMarkOccurrenceController {

    private final MarkOccurrenceService service;
    private final MarkOccurrenceQueryService queryService;
    private final MarkOccurrenceMapper mapper;
    private final UserService userService;

    @GetMapping()
    public ResponseEntity<Page<MarkOccurrenceDto>> getMarkOccurrencesManagement(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(queryService.findAllManagement(pageable).map(mapper::toDto));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkOccurrenceDto> createMarkOccurrence(
            @RequestPart("data") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @Valid MarkOccurrenceRequestDto markOccurrenceDto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal AuthenticatedPrincipal principal
    ) throws IOException {
        MarkOccurrence markOccurrence = mapper.toEntity(markOccurrenceDto);

        User author = userService.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        markOccurrence.setAuthor(author);

        MarkOccurrence createdMarkOccurrence = service.create(markOccurrence, file, markOccurrenceDto.coverId());
        MarkOccurrenceDto response = mapper.toDto(createdMarkOccurrence);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkOccurrenceDto> updateMarkOccurrence(
            @PathVariable Long id,
            @RequestPart("data") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @Valid MarkOccurrenceRequestDto markOccurrenceDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        MarkOccurrence existingMarkOccurrence = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarkOccurrence not found"));

        mapper.updateEntityFromDto(markOccurrenceDto, existingMarkOccurrence);

        MarkOccurrence updatedMarkOccurrence = service.update(existingMarkOccurrence, file, markOccurrenceDto.coverId());
        return ResponseEntity.ok(mapper.toDto(updatedMarkOccurrence));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMarkOccurrence(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkOccurrenceDto> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        MarkOccurrence existingMarkOccurrence = service.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarkOccurrence not found"));

        MarkOccurrence updatedMarkOccurrence = service.update(existingMarkOccurrence, file, null);

        return ResponseEntity.ok(mapper.toDto(updatedMarkOccurrence));
    }
}
