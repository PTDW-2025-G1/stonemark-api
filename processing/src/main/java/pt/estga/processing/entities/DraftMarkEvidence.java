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
    // Use wrapper Float[] to avoid JPA issues with primitive arrays; the column holds embedding vector data.
    private Float[] embedding;

    @Column(columnDefinition = "TEXT")
    private String aiMetadataJson;

    private Boolean aiFlagged;

    private Integer version;
    private Boolean active;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @Column(columnDefinition = "TEXT")
    private String processingError;

    /**
     * Determine whether the draft is ready to be sent to review.
     * A draft is considered ready when processing completed successfully and
     * the draft is still active.
     *
     * @return true when the draft should be presented for review
     */
    public boolean isReadyForReview() {
        return Boolean.TRUE.equals(this.active) && this.processingStatus == ProcessingStatus.COMPLETED;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private MarkEvidenceSubmission submission;

    @ManyToOne
    private MarkOccurrence suggestedOccurrence;

}