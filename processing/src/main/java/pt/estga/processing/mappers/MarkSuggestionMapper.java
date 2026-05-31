package pt.estga.processing.mappers;

import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.entities.MarkSuggestion;

public class MarkSuggestionMapper {

    private MarkSuggestionMapper() {}

    public static MarkSuggestionDto toDto(MarkSuggestion suggestion) {
        if (suggestion == null) return null;
        MarkSuggestionDto dto = new MarkSuggestionDto();
        dto.setId(suggestion.getId());
        dto.setProcessingId(suggestion.getProcessing() != null ? suggestion.getProcessing().getId() : null);
        dto.setMarkId(suggestion.getMarkId());
        dto.setConfidence(suggestion.getConfidence());
        return dto;
    }
}
