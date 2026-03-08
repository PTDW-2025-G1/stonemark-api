package pt.estga.submission.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.submission.dtos.ProposalAdminListDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.enums.SubmissionType;

@Mapper(componentModel = "spring")
public interface SubmissionAdminMapper {

    @Mapping(target = "title", source = "submission", qualifiedByName = "generateTitle")
    @Mapping(target = "photoId", source = "submission", qualifiedByName = "extractPhotoId")
    @Mapping(target = "submittedByUsername", source = "submittedBy.username")
    @Mapping(target = "submissionType", source = "submission", qualifiedByName = "determineType")
    ProposalAdminListDto toAdminListDto(Submission submission);

    @Named("generateTitle")
    default String generateTitle(Submission submission) {
        if (submission instanceof MarkOccurrenceSubmission) {
            return "Mark Occurrence #" + submission.getId();
        }
        return "Submission #" + submission.getId();
    }

    @Named("extractPhotoId")
    default Long extractPhotoId(Submission submission) {
        if (submission instanceof MarkOccurrenceSubmission p && p.getOriginalMediaFile() != null) {
            return p.getOriginalMediaFile().getId();
        }
        return null;
    }

    @Named("determineType")
    default SubmissionType determineType(Submission submission) {
        return submission.getType();
    }
}
