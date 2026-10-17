package pt.estga.intake.events;

import java.util.UUID;

public record MarkEvidenceSubmittedEvent(
        UUID submissionId
) {}
