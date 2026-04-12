package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.processing.enums.ProcessingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceProcessing {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne
    @JoinColumn(unique = true)
    private MarkEvidenceSubmission submission;

    @Column(columnDefinition = "vector(384)")
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private Instant processedAt;

    private Instant failedAt;

    private String errorMessage;

    @OneToMany(mappedBy = "processing")
    List<MarkSuggestion> suggestions;

    /**
     * True when processing is reviewable (COMPLETED or REVIEW_PENDING).
     * This name conveys domain intent more explicitly than "isReadyForReview".
     * Centralizes readiness logic to reduce coupling to enum changes.
     */
    public boolean isReviewable() {
        return status != null && (status == ProcessingStatus.COMPLETED || status == ProcessingStatus.REVIEW_PENDING);
    }

    /**
     * Domain method to mark this processing as reviewed.
     * Encapsulates state transition so callers don't manipulate the enum directly.
     */
    public void markReviewed() {
        // Idempotent: only change status when necessary.
        if (this.status != ProcessingStatus.REVIEWED) {
            this.status = ProcessingStatus.REVIEWED;
        }
    }
}