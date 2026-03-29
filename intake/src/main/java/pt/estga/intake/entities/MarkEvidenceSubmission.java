package pt.estga.intake.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import pt.estga.file.entities.MediaFile;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.user.entities.User;

import java.time.Instant;

@Entity
@Table(name = "mark_occurrence_submission", indexes = {
        @Index(name = "idx_submission_submitted_by", columnList = "submitted_by_id"),
        @Index(name = "idx_submission_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private MediaFile originalMediaFile;

    @Column(columnDefinition = "vector")
    private float[] embedding;

    private Double latitude;
    private Double longitude;

    private String userNotes;

    @Enumerated(EnumType.STRING)
    private SubmissionSource submissionSource;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    private User submittedBy;

    @CreatedDate
    private Instant submittedAt;

}
