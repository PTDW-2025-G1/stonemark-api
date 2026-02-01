package pt.estga.report.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.report.dtos.ReportRequestDto;
import pt.estga.report.dtos.ReportResponseDto;
import pt.estga.report.services.ReportService;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

@RestController
@RequestMapping("/api/v1/public/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Endpoints for content reports.")
public class ReportController {

    private final ReportService service;
    private final UserService userService;

    @PostMapping
    public ReportResponseDto createReport(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid
            @RequestBody ReportRequestDto dto
    ) {
        User user = userService.findById(principal.getId()).orElseThrow();
        return service.createReport(user, dto);
    }
}
