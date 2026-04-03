package pt.estga.review.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.user.entities.User;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DraftReview {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private DraftMarkEvidence draft;

    @Enumerated(EnumType.STRING)
    private ReviewDecision decision;

    private String reviewerNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewedBy;

    private Instant reviewedAt;

}