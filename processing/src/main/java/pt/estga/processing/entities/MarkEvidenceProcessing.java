package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
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
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(unique = true)
    private UUID submissionId;

    /** Feature embedding for similarity search, with the model that produced it. */
    @Column(columnDefinition = "vector(384)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] embedding;

    @Column(length = 64)
    private String embeddingModel;

    @Column(length = 64)
    private String embeddingVersion;

    private Integer embeddingDim;

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