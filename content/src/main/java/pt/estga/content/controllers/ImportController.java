package pt.estga.content.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.territory.services.DivisionImportService;
import pt.estga.content.services.MonumentImportService;
import pt.estga.shared.dtos.MessageResponseDto;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('MODERATOR')")
@RequiredArgsConstructor
@Tag(name = "Imports", description = "Endpoints for importing data.")
public class ImportController {

    private final DivisionImportService divisionImportService;
    private final MonumentImportService monumentImportService;

    @PostMapping(value = "/divisions/import/pbf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> importDivisionsFromPbf(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponseDto("File is empty"));
        }

        int count;
        try (InputStream is = file.getInputStream()) {
            count = divisionImportService.importFromPbf(is);
        }

        return ResponseEntity.ok(new MessageResponseDto("Administrative divisions fully replaced. Imported " + count + " entries."));
    }

    @PostMapping(value = "/monuments/import/geojson", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> importMonumentsFromGeoJson(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponseDto("File is empty"));
        }

        int count;
        try (InputStream is = file.getInputStream()) {
            count = monumentImportService.importFromGeoJson(is);
        }

        return ResponseEntity.ok(new MessageResponseDto("Imported " + count + " monuments successfully."));
    }
}
