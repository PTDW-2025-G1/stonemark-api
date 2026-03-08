package pt.estga.proposal.dtos;

import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.enums.SubmissionSource;

public record MarkOccurrenceProposalDto(
        Long id,
        Integer priority,
        Double latitude,
        Double longitude,
        boolean newMark,
        String userNotes,
        SubmissionSource submissionSource,
        ProposalStatus status,
        Long existingMonumentId,
        String existingMonumentName,
        Long existingMarkId,
        String existingMarkName,
        Long photoId
) { }
