package pt.estga.intake.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.services.SubmissionFacade;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/public/submissions")
@RequiredArgsConstructor
@Tag(name = "Public Submissions", description = "Endpoints for submitting mark evidence from public UI.")
public class SubmissionPublicController {

    private final SubmissionFacade submissionFacade;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDto> submit(
            @Parameter(description = "Photo of the mark evidence", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Latitude coordinate")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "Longitude coordinate")
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String notes) throws IOException {

        SubmissionDto result = submissionFacade.submitFromWeb(file, latitude, longitude, notes);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity.created(location).body(result);
    }
}
