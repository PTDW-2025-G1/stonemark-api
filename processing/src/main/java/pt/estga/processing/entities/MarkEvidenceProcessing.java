package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.processing.enums.ProcessingStatus;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceProcessing {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(unique = true)
    private MarkEvidenceSubmission submission;

    private float[] embedding;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private Instant processedAt;

    private Instant failedAt;

    private String errorMessage;

}