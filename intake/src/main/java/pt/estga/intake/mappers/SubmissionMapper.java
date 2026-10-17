package pt.estga.intake.mappers;

import org.springframework.stereotype.Component;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;

@Component
public class SubmissionMapper {

    public SubmissionDto toDto(MarkEvidenceSubmission s) {
        if (s == null) return null;
        return new SubmissionDto(
                s.getId(),
                s.getStatus(),
                s.getSubmissionSource(),
                s.getSubmittedAt(),
                s.getLatitude(),
                s.getLongitude(),
                s.getUserNotes(),
                s.getOriginalMediaFileId(),
                s.getSubmittedById()
        );
    }
}
