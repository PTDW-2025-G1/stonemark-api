package pt.estga.submission.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.content.mappers.MarkMapper;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.submission.dtos.MarkOccurrenceProposalDto;
import pt.estga.submission.dtos.MarkOccurrenceProposalListDto;
import pt.estga.submission.dtos.ProposalWithRelationsDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

@Mapper(componentModel = "spring", uses = {MarkMapper.class, MonumentMapper.class})
public interface MarkOccurrenceSubmissionMapper {

    @Mapping(source = "originalMediaFile.id", target = "photoId")
    @Mapping(target = "title", source = "entity", qualifiedByName = "generateTitle")
    MarkOccurrenceProposalListDto toListDto(MarkOccurrenceSubmission entity);

    @Mapping(source = "originalMediaFile.id", target = "photoId")
    @Mapping(source = "existingMonument.id", target = "existingMonumentId")
    @Mapping(source = "existingMonument.name", target = "existingMonumentName")
    @Mapping(source = "existingMark.id", target = "existingMarkId")
    MarkOccurrenceProposalDto toDto(MarkOccurrenceSubmission entity);

    ProposalWithRelationsDto toWithRelationsDto(MarkOccurrenceSubmission entity);

    @Named("generateTitle")
    default String generateTitle(MarkOccurrenceSubmission proposal) {
        return "Submission #" + proposal.getId();
    }
}
