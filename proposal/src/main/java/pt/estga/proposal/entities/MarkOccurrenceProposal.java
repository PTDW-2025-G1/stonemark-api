package pt.estga.proposal.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.Monument;
import pt.estga.file.entities.MediaFile;
import pt.estga.proposal.enums.ProposalType;
import pt.estga.shared.utils.PgVectorType;

@Entity
@DiscriminatorValue("MARK_OCCURRENCE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MarkOccurrenceProposal extends Proposal {

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

    @Override
    public ProposalType getType() {
        return ProposalType.MARK_OCCURRENCE;
    }
}
