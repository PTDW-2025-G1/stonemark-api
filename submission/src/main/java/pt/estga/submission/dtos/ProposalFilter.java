package pt.estga.submission.dtos;

import pt.estga.submission.enums.SubmissionStatus;

import java.util.List;

public record ProposalFilter(
        List<SubmissionStatus> statuses,
        Long submittedById,
        Long existingMonumentId,
        Long existingMarkId
) { }
