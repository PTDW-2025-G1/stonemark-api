package pt.estga.review.dtos;

import lombok.Builder;
import lombok.Data;
import pt.estga.review.enums.ReviewDecision;

import java.time.Instant;

@Data
@Builder
public class ReviewResponseDto {
    private Long id;
    private Long submissionId;
    private ReviewDecision decision;
    private Long selectedMarkId;
    private String selectedMarkTitle;
    private String reviewerName;
    private Instant reviewedAt;
    private String comment;
}
