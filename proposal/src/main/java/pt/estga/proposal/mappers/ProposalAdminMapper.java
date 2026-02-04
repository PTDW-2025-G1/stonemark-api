package pt.estga.proposal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.proposal.dtos.ProposalAdminListDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.MarkProposal;
import pt.estga.proposal.entities.MonumentProposal;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.enums.ProposalType;

@Mapper(componentModel = "spring")
public interface ProposalAdminMapper {

    @Mapping(target = "title", source = "proposal", qualifiedByName = "generateTitle")
    @Mapping(target = "photoId", source = "proposal", qualifiedByName = "extractPhotoId")
    @Mapping(target = "submittedByUsername", source = "submittedBy.username")
    @Mapping(target = "proposalType", source = "proposal", qualifiedByName = "determineType")
    ProposalAdminListDto toAdminListDto(Proposal proposal);

    @Named("generateTitle")
    default String generateTitle(Proposal proposal) {
        if (proposal instanceof MarkOccurrenceProposal) {
            return "Mark Occurrence #" + proposal.getId();
        } else if (proposal instanceof MonumentProposal) {
            return "New Monument #" + proposal.getId();
        } else if (proposal instanceof MarkProposal) {
            return "New Mark #" + proposal.getId();
        }
        return "Proposal #" + proposal.getId();
    }

    @Named("extractPhotoId")
    default Long extractPhotoId(Proposal proposal) {
        if (proposal instanceof MarkOccurrenceProposal p && p.getOriginalMediaFile() != null) {
            return p.getOriginalMediaFile().getId();
        } else if (proposal instanceof MarkProposal p && p.getCoverImage() != null) {
            return p.getCoverImage().getId();
        }
        return null;
    }

    @Named("determineType")
    default ProposalType determineType(Proposal proposal) {
        return proposal.getType();
    }
}
