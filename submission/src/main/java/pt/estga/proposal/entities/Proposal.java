package pt.estga.proposal.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.enums.ProposalType;
import pt.estga.proposal.enums.SubmissionSource;
import pt.estga.shared.audit.AuditedEntity;
import pt.estga.user.entities.User;

import java.time.Instant;

@Entity
@Table(name = "proposal", indexes = {
        @Index(name = "idx_proposal_submitted_by", columnList = "submitted_by_id"),
        @Index(name = "idx_proposal_status", columnList = "status")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "proposal_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class Proposal extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userNotes;

    @Enumerated(EnumType.STRING)
    private SubmissionSource submissionSource;

    private Integer priority;

    private Integer credibilityScore;

    @ManyToOne(fetch = FetchType.LAZY)
    private User submittedBy;

    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    private ProposalStatus status;

    public abstract ProposalType getType();
}
