package pt.estga.proposal.dtos;

import pt.estga.proposal.enums.ProposalStatus;

import java.time.Instant;

public record ProposalSummaryDto(
        Long id,
        String title,
        String type,
        Long photoId,
        ProposalStatus status,
        Instant submittedAt
) {}
