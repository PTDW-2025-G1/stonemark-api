package pt.estga.proposal.dtos;

import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.enums.ProposalType;
import pt.estga.proposal.enums.SubmissionSource;

import java.time.Instant;

public record ProposalAdminListDto(
        Long id,
        ProposalStatus status,
        ProposalType proposalType,
        Integer priority,
        String title,
        Long photoId,
        String submittedByUsername,
        SubmissionSource submissionSource,
        Instant submittedAt
) {
}
