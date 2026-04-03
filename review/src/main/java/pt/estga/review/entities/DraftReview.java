package pt.estga.review.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
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

    /**
     * Optional reviewer id for simpler storage when user entity is not available.
     */
    private Long reviewerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @CreatedBy
    private User reviewedBy;

    @CreatedDate
    private Instant reviewedAt;

}