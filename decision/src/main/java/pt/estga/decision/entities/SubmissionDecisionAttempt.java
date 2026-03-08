package pt.estga.decision.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.submission.entities.Submission;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.user.entities.User;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SubmissionDecisionAttempt {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private DecisionType type;

    @Enumerated(EnumType.STRING)
    private DecisionOutcome outcome;

    private Boolean confident;

    private String notes;

    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    private User decidedBy;

}
