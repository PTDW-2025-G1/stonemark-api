package pt.estga.decision.dtos;

import pt.estga.submission.dtos.ProposalWithRelationsDto;

public record ProposalWithDecisionDto(
        ProposalWithRelationsDto proposal,
        ActiveDecisionViewDto activeDecision
) {
}
