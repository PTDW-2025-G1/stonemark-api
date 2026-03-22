package pt.estga.contentimport;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.contentimport.services.DivisionImportService;
import pt.estga.contentimport.services.MonumentImportService;
import pt.estga.sharedweb.dtos.MessageResponseDto;

import java.io.InputStream;

@Slf4j
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
        log.info("Starting division import from PBF file. File name: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            log.warn("Empty file received for division import");
            return ResponseEntity.badRequest().body(MessageResponseDto.error("File is empty"));
        }

        try {
            int count;
            try (InputStream is = file.getInputStream()) {
                log.debug("Opening input stream for file: {}", file.getOriginalFilename());
                count = divisionImportService.importFromPbf(is);
                log.info("Successfully imported {} divisions from PBF file", count);
            }

            String message = "Administrative divisions fully replaced. Imported " + count + " entries.";
            log.info("Division import completed successfully: {}", message);
            return ResponseEntity.ok(MessageResponseDto.success(message));
        } catch (Exception e) {
            log.error("Error importing divisions from PBF file: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    @PostMapping(value = "/monuments/import/geojson", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> importMonumentsFromGeoJson(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        log.info("Starting monument import from GeoJSON file. File name: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            log.warn("Empty file received for monument import");
            return ResponseEntity.badRequest().body(MessageResponseDto.error("File is empty"));
        }

        try {
            int count;
            try (InputStream is = file.getInputStream()) {
                log.debug("Opening input stream for file: {}", file.getOriginalFilename());
                count = monumentImportService.importFromGeoJson(is);
                log.info("Successfully imported {} monuments from GeoJSON file", count);
            }

            String message = "Imported " + count + " monuments successfully.";
            log.info("Monument import completed successfully: {}", message);
            return ResponseEntity.ok(MessageResponseDto.success(message));
        } catch (Exception e) {
            log.error("Error importing monuments from GeoJSON file: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }
}
