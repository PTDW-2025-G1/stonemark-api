package pt.estga.processing.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class MarkSuggestionDto {
    private UUID id;
    private UUID processingId;
    private Long markId;
    private double confidence;
}
