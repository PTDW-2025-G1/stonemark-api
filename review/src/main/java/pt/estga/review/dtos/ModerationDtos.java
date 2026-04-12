package pt.estga.review.dtos;

import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.shared.enums.ValidationState;

import java.util.UUID;

public final class ModerationDtos {

    public record ProcessingDto(UUID processingId, Long submissionId, ProcessingStatus status, Double maxConfidence) {}

    public record MarkDto(Long id, String title, ValidationState validationState) {}

    public record MonumentDto(Long id, String name, ValidationState validationState) {}

}
