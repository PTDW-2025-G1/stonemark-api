package pt.estga.mark.entities;

import jakarta.persistence.*;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.audit.AuditedEntity;

@Entity
public class MarkEvidence extends AuditedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private MediaFile file;

    @Column(columnDefinition = "vector")
    private float[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    private MarkOccurrence occurrence;
}