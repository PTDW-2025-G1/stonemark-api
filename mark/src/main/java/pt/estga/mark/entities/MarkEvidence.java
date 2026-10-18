package pt.estga.mark.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import pt.estga.shared.jpa.entities.SoftDeletableEntity;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidence extends SoftDeletableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column
    private UUID fileId;

    /** Submission that produced this evidence, when it originated from intake. */
    @Column(name = "submission_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    private MarkOccurrence occurrence;
}