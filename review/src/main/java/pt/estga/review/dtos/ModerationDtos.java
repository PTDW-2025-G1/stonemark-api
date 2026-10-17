package pt.estga.review.dtos;

import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.shared.core.enums.PublicationStatus;

import java.util.UUID;

public final class ModerationDtos {

    public record ProcessingDto(UUID processingId, UUID submissionId, ProcessingStatus status, Double maxConfidence) {}

    public record MarkDto(UUID id, String title, PublicationStatus status) {}

}
