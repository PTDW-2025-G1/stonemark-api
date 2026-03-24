package pt.estga.file.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.mappers.MediaFileMapper;
import pt.estga.file.application.MediaService;
import pt.estga.file.application.MediaVariantService;
import pt.estga.sharedweb.exceptions.FileNotFoundException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Endpoints for managing and retrieving media files.")
public class MediaController {

    private final MediaService mediaService;
    private final MediaVariantService mediaVariantService;
    private final MediaFileMapper mediaFileMapper;

    @Operation(summary = "Upload a media file", description = "Uploads a media file and returns its metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = MediaFileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or file upload error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaFileDto> uploadMedia(
            @Parameter(description = "The file to upload", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Request to upload media file: {}", file.getOriginalFilename());
        MediaFile mediaFile = mediaService.save(file.getInputStream(), file.getOriginalFilename());
        return ResponseEntity.ok(mediaFileMapper.toDto(mediaFile));
    }

    @Operation(summary = "Get media file by ID", description = "Retrieves the original media file by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Media file not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getMediaById(
            @Parameter(description = "ID of the media file", required = true)
            @PathVariable Long id) {
        log.info("Request to get media with id: {}", id);

        // Fetch metadata first
        MediaFile mediaFile = mediaService.findById(id)
                .orElseThrow(() -> {
                    log.warn("Media not found with id: {}", id);
                    return new FileNotFoundException("Media not found with id: " + id);
                });

        if (mediaFile.getStoragePath() == null || mediaFile.getStoragePath().isEmpty()) {
            log.warn("MediaFile with id {} has no storage path.", id);
            throw new FileNotFoundException("Media file has no storage path");
        }

        log.info("Found media file, loading from path: {}", mediaFile.getStoragePath());
        
        // Use the new method to load resource directly from entity
        Resource resource = mediaService.loadFile(mediaFile);

        MediaType mediaType = MediaTypeFactory.getMediaType(mediaFile.getOriginalFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        String extension = StringUtils.getFilenameExtension(mediaFile.getOriginalFilename());
        String filename = "stonemark-" + mediaFile.getId() + (extension != null ? "." + extension : "");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic()) // Cache for 1 year
                .body(resource);
    }

    @Operation(summary = "Get media thumbnail", description = "Retrieves the thumbnail variant of the media file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thumbnail retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Thumbnail not found")
    })
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(
            @Parameter(description = "ID of the media file", required = true)
            @PathVariable Long id) {
        return getVariant(id, MediaVariantType.THUMBNAIL);
    }

    @Operation(summary = "Get media preview", description = "Retrieves the preview variant of the media file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preview retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Preview not found")
    })
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> getPreview(
            @Parameter(description = "ID of the media file", required = true)
            @PathVariable Long id) {
        return getVariant(id, MediaVariantType.PREVIEW);
    }

    @Operation(summary = "Get optimized media", description = "Retrieves the optimized variant of the media file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Optimized version retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Optimized version not found")
    })
    @GetMapping("/{id}/optimized")
    public ResponseEntity<Resource> getOptimized(
            @Parameter(description = "ID of the media file", required = true)
            @PathVariable Long id) {
        return getVariant(id, MediaVariantType.OPTIMIZED);
    }

    private ResponseEntity<Resource> getVariant(Long id, MediaVariantType type) {
        Resource resource = mediaVariantService.loadVariant(id, type);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/webp"))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic()) // Cache for 1 year
                .body(resource);
    }
}
