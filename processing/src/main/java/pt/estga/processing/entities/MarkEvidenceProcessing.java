package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
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

    @Column(unique = true)
    private Long submissionId;

    @Column(columnDefinition = "vector(384)")
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private Instant processedAt;

    private Instant failedAt;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 5;

    private Instant lastRetryAt;

    @Builder.Default
    private boolean permanent = false;

    private Instant updatedAt;

    private Instant processingStartedAt;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @OneToMany(mappedBy = "processing")
    List<MarkSuggestion> suggestions;

}