package pt.estga.proposal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.content.mappers.MarkMapper;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.proposal.dtos.MarkOccurrenceProposalDto;
import pt.estga.proposal.dtos.MarkOccurrenceProposalListDto;
import pt.estga.proposal.dtos.ProposalWithRelationsDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;

@Mapper(componentModel = "spring", uses = {MarkMapper.class, MonumentMapper.class})
public interface MarkOccurrenceProposalMapper {

    @Mapping(source = "originalMediaFile.id", target = "photoId")
    @Mapping(target = "title", source = "entity", qualifiedByName = "generateTitle")
    MarkOccurrenceProposalListDto toListDto(MarkOccurrenceProposal entity);

    @Mapping(source = "originalMediaFile.id", target = "photoId")
    @Mapping(source = "existingMonument.id", target = "existingMonumentId")
    @Mapping(source = "existingMonument.name", target = "existingMonumentName")
    @Mapping(source = "existingMark.id", target = "existingMarkId")
    MarkOccurrenceProposalDto toDto(MarkOccurrenceProposal entity);

    ProposalWithRelationsDto toWithRelationsDto(MarkOccurrenceProposal entity);

    @Named("generateTitle")
    default String generateTitle(MarkOccurrenceProposal proposal) {
        return "Proposal #" + proposal.getId();
    }
}
