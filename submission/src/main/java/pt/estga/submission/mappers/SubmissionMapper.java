package pt.estga.submission.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.submission.dtos.ProposalSummaryDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    @Mapping(target = "title", source = "submission", qualifiedByName = "generateTitle")
    @Mapping(target = "photoId", source = "submission", qualifiedByName = "extractPhotoId")
    ProposalSummaryDto toSummaryDto(MarkOccurrenceSubmission submission);

    @Named("generateTitle")
    default String generateTitle(MarkOccurrenceSubmission submission) {
        return "Mark Occurrence #" + submission.getId();
    }

    @Named("extractPhotoId")
    default Long extractPhotoId(MarkOccurrenceSubmission submission) {
        if (submission.getOriginalMediaFile() != null) {
            return submission.getOriginalMediaFile().getId();
        }
        return null;
    }
}
