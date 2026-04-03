package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.shared.entities.BaseEntity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DraftMarkEvidence extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "vector")
    private float[] embedding;

    @Column(columnDefinition = "TEXT")
    private String aiMetadataJson;

    private Boolean aiFlagged;

    private Integer version;
    private Boolean active;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private MarkEvidenceSubmission submission;

    @ManyToOne
    private MarkOccurrence suggestedOccurrence;

}