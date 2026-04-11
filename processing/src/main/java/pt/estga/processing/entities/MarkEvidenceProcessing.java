package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.shared.utils.VectorConverter;

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

    @Convert(converter = VectorConverter.class)
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private Instant processedAt;

    private Instant failedAt;

    private String errorMessage;

    @OneToMany(mappedBy = "processing")
    List<MarkSuggestion> suggestions;

    /**
     * True when processing is ready for manual review (COMPLETED or REVIEW_PENDING).
     * Centralizes readiness logic to reduce coupling to enum changes.
     */
    public boolean isReadyForReview() {
        return status != null && (status == ProcessingStatus.COMPLETED || status == ProcessingStatus.REVIEW_PENDING);
    }
}