package pt.estga.intake.dtos;

import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.enums.SubmissionType;
import pt.estga.intake.enums.SubmissionSource;

import java.time.Instant;

public record ProposalAdminListDto(
        Long id,
        SubmissionStatus status,
        SubmissionType submissionType,
        Integer priority,
        String title,
        Long photoId,
        String submittedByUsername,
        SubmissionSource submissionSource,
        Instant submittedAt
) {
}
