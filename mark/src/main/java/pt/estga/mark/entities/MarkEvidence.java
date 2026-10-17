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
import org.hibernate.annotations.UuidGenerator;
import pt.estga.shared.jpa.entities.BaseEntity;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidence extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column
    private UUID fileId;

    /** Submission that produced this evidence, when it originated from intake. */
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(columnDefinition = "vector(384)")
    private float[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    private MarkOccurrence occurrence;
}