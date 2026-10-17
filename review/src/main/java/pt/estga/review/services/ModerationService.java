package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.review.dtos.ModerationDtos;
import pt.estga.shared.core.enums.ValidationState;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationService {

    private final ProcessingReviewApi processingApi;
    private final MarkQueryService markQueryService;

    public List<ModerationDtos.ProcessingDto> findProcessingByConfidence(double min, double max) {
        return processingApi.findProcessingByConfidence(min, max).stream()
                .map(v -> new ModerationDtos.ProcessingDto(
                        v.processingId(), v.submissionId(), v.status(), v.maxConfidence()))
                .toList();
    }

    public List<ModerationDtos.MarkDto> findProvisionalMarks() {
        return markQueryService.findProvisionalMarks().stream()
                .map(m -> new ModerationDtos.MarkDto(m.id(), m.title(), ValidationState.PROVISIONAL))
                .toList();
    }
}
