package pt.estga.review.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkOccurrence;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkReviewDecision {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private MarkEvidenceSubmission submission;

    @ManyToOne
    private Mark selectedMark;

    @ManyToOne
    private MarkOccurrence selectedOccurrence;

    private boolean createNewMark;

    private boolean approved;

    private Instant decidedAt;
}