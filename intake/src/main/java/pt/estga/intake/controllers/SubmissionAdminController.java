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
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.intake.services.SubmissionService;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/submissions")
@RequiredArgsConstructor
@Tag(name = "Submission Management", description = "Admin endpoints for managing evidence submissions.")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class SubmissionAdminController {

    private final SubmissionService submissionService;
    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;

    @GetMapping
    public ResponseEntity<Page<SubmissionDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) SubmissionSource source,
            @RequestParam(required = false) Long submittedById,
            @RequestParam(required = false) Instant submittedAfter,
            @RequestParam(required = false) Instant submittedBefore,
            @RequestParam(required = false) String divisionCode) {
        SubmissionFilter filter = new SubmissionFilter(status, source, submittedById, submittedAfter, submittedBefore, divisionCode);
        return ResponseEntity.ok(submissionService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> findById(@PathVariable Long id) {
        return submissionRepository.findById(id)
                .map(submissionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
