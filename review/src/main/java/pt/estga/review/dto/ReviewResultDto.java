package pt.estga.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pt.estga.review.enums.ReviewDecision;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultDto {
    private Long submissionId;
    private ReviewDecision decision;
    private Long selectedMarkId;
    private Instant reviewedAt;
}
