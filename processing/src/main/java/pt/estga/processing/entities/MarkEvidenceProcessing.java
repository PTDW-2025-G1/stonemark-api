package pt.estga.processing.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.*;
import pt.estga.intake.entities.MarkEvidenceSubmission;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceProcessing {

    @Id
    private Long id;

    @OneToOne
    private MarkEvidenceSubmission submission;

    private float[] embedding;

    private Instant processedAt;

    public boolean isProcessed() {
        return embedding != null && processedAt != null;
    }
}