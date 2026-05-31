package pt.estga.review.dtos;

import pt.estga.processing.enums.ReviewGroupStatus;

import java.util.List;

public record GroupResponseDto(
        Long id,
        ReviewGroupStatus status,
        Integer memberCount,
        Double centroidLatitude,
        Double centroidLongitude,
        Double radiusMeters,
        Integer decision,
        List<Long> submissionIds) {
}
