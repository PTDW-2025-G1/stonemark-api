package pt.estga.processing.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.ProcessingStatusDto;
import pt.estga.processing.services.processing.ProcessingStatusQueryService;

@RestController
@RequestMapping("/api/v1/admin/processing")
@RequiredArgsConstructor
@Tag(name = "Processing Status", description = "Endpoints for checking processing status of submissions.")
@PreAuthorize("hasAuthority('PROCESSING_VIEW')")
public class ProcessingController {

    private final ProcessingStatusQueryService processingStatusQueryService;

    @GetMapping("/submission/{submissionId}/status")
    public ResponseEntity<ProcessingStatusDto> getProcessingStatus(@PathVariable Long submissionId) {
        return processingStatusQueryService.findStatusBySubmissionId(submissionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
