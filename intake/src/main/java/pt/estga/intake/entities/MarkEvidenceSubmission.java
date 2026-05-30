package pt.estga.intake.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "idx_evidence_submitted_by", columnList = "submitted_by_id"),
        @Index(name = "idx_evidence_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private UUID originalMediaFileId;

    private Double latitude;
    private Double longitude;

    private String userNotes;

    @Enumerated(EnumType.STRING)
    private SubmissionSource submissionSource;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column
    private Long submittedById;

    @CreatedDate
    private Instant submittedAt;

    /**
     * Domain method: mark submission as processed by a human reviewer.
     */
    public void markProcessed() {
        this.status = SubmissionStatus.PROCESSED;
    }

    /**
     * Domain method: mark submission as rejected by a human reviewer.
     */
    public void markRejected() {
        this.status = SubmissionStatus.REJECTED;
    }

}
