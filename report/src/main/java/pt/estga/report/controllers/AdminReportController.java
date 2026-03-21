package pt.estga.report.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.report.dtos.ReportResponseDto;
import pt.estga.report.enums.ReportStatus;
import pt.estga.report.mappers.ReportMapper;
import pt.estga.report.services.ReportService;
import pt.estga.shared.filters.models.FilterNode;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Endpoints for content reports and moderation.")
@PreAuthorize("hasRole('MODERATOR')")
public class AdminReportController {

    private final ReportService service;
    private final ReportMapper mapper;

    @PatchMapping("/{reportId}/status")
    public ReportResponseDto updateStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status
    ) {
        return service.updateStatus(reportId, status);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ReportResponseDto>> search(
            @RequestBody FilterNode filter,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.search(filter, pageable).map(mapper::toDto));
    }
}
