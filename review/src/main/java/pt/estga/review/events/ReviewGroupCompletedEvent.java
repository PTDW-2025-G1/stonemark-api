package pt.estga.review.events;

import lombok.Builder;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.models.ResolutionResult;

import java.util.List;

@Builder
public record ReviewGroupCompletedEvent(
        Long groupId,
        ReviewDecision decision,
        List<GroupMemberResult> members
) {

    @Builder
    public record GroupMemberResult(
            Long submissionId,
            ResolutionResult resolution
    ) {}
}
