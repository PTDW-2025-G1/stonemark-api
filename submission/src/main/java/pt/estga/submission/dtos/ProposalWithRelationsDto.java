package pt.estga.submission.dtos;

import pt.estga.content.dtos.MarkDto;
import pt.estga.content.dtos.MonumentDto;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.enums.SubmissionSource;
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
