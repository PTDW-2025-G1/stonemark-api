package pt.estga.submission.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.monument.MonumentMapper;
import pt.estga.submission.dtos.ProposalWithRelationsDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

@Mapper(componentModel = "spring", uses = {MarkMapper.class, MonumentMapper.class})
public interface MarkOccurrenceSubmissionMapper {

    ProposalWithRelationsDto toWithRelationsDto(MarkOccurrenceSubmission entity);

    @Named("generateTitle")
    default String generateTitle(MarkOccurrenceSubmission proposal) {
        return "Submission #" + proposal.getId();
    }
}
