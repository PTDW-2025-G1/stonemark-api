package pt.estga.review.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.mark.entities.Mark;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.user.entities.User;

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

    @OneToOne
    @JoinColumn(unique = true)
    private MarkEvidenceSubmission submission;

    @ManyToOne
    private Mark selectedMark;

    private ReviewDecision decision;

    private Instant reviewedAt;

    @ManyToOne
    private User reviewedBy;

    String comment;
}