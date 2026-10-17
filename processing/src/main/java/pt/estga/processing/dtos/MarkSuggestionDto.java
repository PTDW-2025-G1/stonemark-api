package pt.estga.processing.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class MarkSuggestionDto {
    private UUID id;
    private UUID processingId;
    private UUID markId;
    private double confidence;
}
