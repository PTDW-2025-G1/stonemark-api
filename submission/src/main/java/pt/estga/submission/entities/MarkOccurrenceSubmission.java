package pt.estga.submission.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.Monument;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.utils.PgVectorType;
import pt.estga.submission.enums.SubmissionSource;
import pt.estga.submission.enums.SubmissionStatus;
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
public class MarkOccurrenceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userNotes;

    @Enumerated(EnumType.STRING)
    private SubmissionSource submissionSource;

    private Integer priority;

    private Integer credibilityScore;

    @ManyToOne(fetch = FetchType.LAZY)
    private User submittedBy;

    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mark existingMark;

    @ManyToOne(fetch = FetchType.LAZY)
    private Monument existingMonument;

    @OneToOne(fetch = FetchType.LAZY)
    private MediaFile originalMediaFile;

    @Type(PgVectorType.class)
    @Column(columnDefinition = "vector")
    private float[] embedding;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private boolean newMark = true;
}
