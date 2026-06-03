package pt.estga.review.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.review.dtos.ModerationDtos;
import pt.estga.review.services.ModerationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REVIEW_MODERATE')")
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/processing-by-confidence")
    public ResponseEntity<List<ModerationDtos.ProcessingDto>> processingByConfidence(
            @RequestParam double min,
            @RequestParam double max) {
        return ResponseEntity.ok(moderationService.findProcessingByConfidence(min, max));
    }

    @GetMapping("/discovery/marks")
    public ResponseEntity<List<ModerationDtos.MarkDto>> provisionalMarks() {
        return ResponseEntity.ok(moderationService.findProvisionalMarks());
    }
}
