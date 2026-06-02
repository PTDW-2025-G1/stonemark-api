package pt.estga.review.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.review.converters.ReviewDecisionConverter;
import pt.estga.review.enums.ReviewDecision;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceReview {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private Long submissionId;

    private Long selectedMarkId;

    @Convert(converter = ReviewDecisionConverter.class)
    @Column(name = "decision", nullable = false, columnDefinition = "integer")
    private ReviewDecision decision;

    private Instant reviewedAt;

    private Long reviewedById;

    String comment;
}