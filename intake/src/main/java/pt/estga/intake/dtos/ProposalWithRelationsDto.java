package pt.estga.intake.dtos;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.monument.dots.MonumentDto;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.user.dtos.UserDto;

import java.time.Instant;

public record ProposalWithRelationsDto(
        Long id,
        MarkDto existingMark,
        MonumentDto existingMonument,
        MediaFileDto originalMediaFile,
        String userNotes,
        Double latitude,
        Double longitude,
        SubmissionSource submissionSource,
        Integer priority,
        Integer credibilityScore,
        boolean submitted,
        UserDto submittedBy,
        Instant submittedAt,
        boolean newMark,
        SubmissionStatus status
) {
}
