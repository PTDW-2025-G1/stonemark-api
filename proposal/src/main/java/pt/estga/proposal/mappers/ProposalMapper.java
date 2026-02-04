package pt.estga.proposal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.proposal.dtos.ProposalSummaryDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.MarkProposal;
import pt.estga.proposal.entities.MonumentProposal;
import pt.estga.proposal.entities.Proposal;

@Mapper(componentModel = "spring")
public interface ProposalMapper {

    @Mapping(target = "title", source = "proposal", qualifiedByName = "generateTitle")
    @Mapping(target = "photoId", source = "proposal", qualifiedByName = "extractPhotoId")
    ProposalSummaryDto toSummaryDto(Proposal proposal);

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
        // MonumentProposal might not have a direct photo field in the same way, or it might be added later
        return null;
    }
}
