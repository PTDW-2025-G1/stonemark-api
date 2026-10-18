package pt.estga.review.entities;

import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

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
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(unique = true)
    private UUID submissionId;

    private UUID selectedMarkId;

    @Convert(converter = ReviewDecisionConverter.class)
    @Column(name = "decision", nullable = false, columnDefinition = "integer")
    private ReviewDecision decision;

    private Instant reviewedAt;

    private UUID reviewedById;

    String comment;
}