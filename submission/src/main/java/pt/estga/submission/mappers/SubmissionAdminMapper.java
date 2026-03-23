package pt.estga.submission.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pt.estga.submission.dtos.ProposalAdminListDto;
import pt.estga.submission.entities.MarkEvidenceSubmission;

@Mapper(componentModel = "spring")
public interface SubmissionAdminMapper {

    @Mapping(target = "title", source = "submission", qualifiedByName = "generateTitle")
    @Mapping(target = "photoId", source = "submission", qualifiedByName = "extractPhotoId")
    @Mapping(target = "submittedByUsername", source = "submittedBy.username")
    @Mapping(target = "submissionType", constant = "MARK_OCCURRENCE")
    ProposalAdminListDto toAdminListDto(MarkEvidenceSubmission submission);

    @Named("generateTitle")
    default String generateTitle(MarkEvidenceSubmission submission) {
        return "Mark Occurrence #" + submission.getId();
    }

    @Named("extractPhotoId")
    default Long extractPhotoId(MarkEvidenceSubmission submission) {
        if (submission.getOriginalMediaFile() != null) {
            return submission.getOriginalMediaFile().getId();
        }
        return null;
    }
}
