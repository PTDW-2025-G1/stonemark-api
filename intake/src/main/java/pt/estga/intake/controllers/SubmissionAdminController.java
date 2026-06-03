package pt.estga.intake.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.dtos.SubmissionFilter;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.services.SubmissionQueryService;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/submissions")
@RequiredArgsConstructor
@Tag(name = "Submission Management", description = "Admin endpoints for managing evidence submissions.")
@PreAuthorize("hasAuthority('SUBMISSIONS_MANAGE')")
public class SubmissionAdminController {

    private final SubmissionQueryService submissionQueryService;

    @GetMapping
    public ResponseEntity<Page<SubmissionDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) SubmissionSource source,
            @RequestParam(required = false) Long submittedById,
            @RequestParam(required = false) Instant submittedAfter,
            @RequestParam(required = false) Instant submittedBefore,
            @RequestParam(required = false) Long divisionId) {
        SubmissionFilter filter = new SubmissionFilter(status, source, submittedById, submittedAfter, submittedBefore, divisionId);
        return ResponseEntity.ok(submissionQueryService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> findById(@PathVariable Long id) {
        return submissionQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
