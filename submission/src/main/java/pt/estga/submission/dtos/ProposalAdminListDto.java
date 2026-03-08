package pt.estga.submission.dtos;

import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.enums.SubmissionType;
import pt.estga.submission.enums.SubmissionSource;

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
