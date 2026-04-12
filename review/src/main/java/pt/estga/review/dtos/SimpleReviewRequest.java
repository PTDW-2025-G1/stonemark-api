package pt.estga.review.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimpleReviewRequest {
    private String comment;
}
