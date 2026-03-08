package pt.estga.submission.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.submission.dtos.ProposalSummaryDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.entities.MarkSubmission;
import pt.estga.submission.entities.Submission;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    @Mapping(target = "title", source = "submission", qualifiedByName = "generateTitle")
    @Mapping(target = "photoId", source = "submission", qualifiedByName = "extractPhotoId")
    ProposalSummaryDto toSummaryDto(Submission submission);

    @Named("generateTitle")
    default String generateTitle(Submission submission) {
        if (submission instanceof MarkOccurrenceSubmission) {
            return "Mark Occurrence #" + submission.getId();
        } else if (submission instanceof MarkSubmission) {
            return "New Mark #" + submission.getId();
        }
        return "Submission #" + submission.getId();
    }

    @Named("extractPhotoId")
    default Long extractPhotoId(Submission submission) {
        if (submission instanceof MarkOccurrenceSubmission p && p.getOriginalMediaFile() != null) {
            return p.getOriginalMediaFile().getId();
        } else if (submission instanceof MarkSubmission p && p.getCoverImage() != null) {
            return p.getCoverImage().getId();
        }
        return null;
    }
}
