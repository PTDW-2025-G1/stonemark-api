package pt.estga.proposal.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import pt.estga.proposal.enums.ProposalType;
import pt.estga.territory.entities.AdministrativeDivision;

@Entity
@DiscriminatorValue("MONUMENT")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MonumentProposal extends Proposal {

    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision parish;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision municipality;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision district;

    @Override
    public ProposalType getType() {
        return ProposalType.MONUMENT;
    }
}
