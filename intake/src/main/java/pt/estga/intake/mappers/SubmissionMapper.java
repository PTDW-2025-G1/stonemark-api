package pt.estga.intake.mappers;

import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;

public class SubmissionMapper {

    private SubmissionMapper() {}

    public static SubmissionDto toDto(MarkEvidenceSubmission s) {
        if (s == null) return null;
        String divisionName = s.getDivision() != null ? s.getDivision().getName() : null;
        Long divisionId = s.getDivision() != null ? s.getDivision().getId() : null;
        return new SubmissionDto(
                s.getId(),
                s.getStatus(),
                s.getSubmissionSource(),
                s.getSubmittedAt(),
                s.getLatitude(),
                s.getLongitude(),
                s.getUserNotes(),
                s.getOriginalMediaFileId(),
                s.getSubmittedById(),
                divisionId,
                divisionName
        );
    }
}
