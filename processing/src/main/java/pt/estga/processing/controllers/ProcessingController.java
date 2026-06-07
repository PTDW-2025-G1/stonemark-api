package pt.estga.processing.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.ProcessingStatusDto;
import pt.estga.processing.services.processing.AsyncProcessingService;
import pt.estga.processing.services.processing.ProcessingStatusQueryService;
import pt.estga.commonweb.dtos.MessageResponseDto;

@RestController
@RequestMapping("/api/v1/admin/processing")
@RequiredArgsConstructor
@Tag(name = "Processing", description = "Endpoints for checking and managing processing of submissions.")
@PreAuthorize("hasAnyAuthority('PROCESSING_VIEW', 'PROCESSING_MANAGE')")
public class ProcessingController {

    private final ProcessingStatusQueryService processingStatusQueryService;
    private final AsyncProcessingService asyncProcessingService;

    @GetMapping("/submission/{submissionId}/status")
    public ResponseEntity<ProcessingStatusDto> getProcessingStatus(@PathVariable Long submissionId) {
        return processingStatusQueryService.findStatusBySubmissionId(submissionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('PROCESSING_MANAGE')")
    @PostMapping("/submission/{submissionId}/reprocess")
    public ResponseEntity<MessageResponseDto> reprocess(@PathVariable Long submissionId) {
        asyncProcessingService.processAsync(submissionId);
        return ResponseEntity.accepted().body(MessageResponseDto.success("Reprocessing triggered for submission " + submissionId));
    }
}
