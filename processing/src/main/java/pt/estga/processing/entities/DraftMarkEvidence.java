package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.mark.entities.MarkOccurrence;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DraftMarkEvidence {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private MarkEvidenceSubmission submission;

    @Column(columnDefinition = "vector")
    private float[] embedding;

    private String aiSuggestions;
    private Boolean aiFlagged;

    @ManyToOne
    private MarkOccurrence occurrence;

    private Instant createdAt;

}