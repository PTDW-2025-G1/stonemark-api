package pt.estga.processing.mappers;

import org.springframework.stereotype.Component;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.entities.MarkSuggestion;

@Component
public class MarkSuggestionMapper {

    public MarkSuggestionDto toDto(MarkSuggestion suggestion) {
        if (suggestion == null) return null;
        MarkSuggestionDto dto = new MarkSuggestionDto();
        dto.setId(suggestion.getId());
        dto.setProcessingId(suggestion.getProcessing() != null ? suggestion.getProcessing().getId() : null);
        dto.setMarkId(suggestion.getMarkId());
        dto.setConfidence(suggestion.getConfidence());
        return dto;
    }
}
