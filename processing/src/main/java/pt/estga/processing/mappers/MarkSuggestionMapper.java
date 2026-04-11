package pt.estga.processing.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.entities.MarkSuggestion;

@Mapper(componentModel = "spring")
public interface MarkSuggestionMapper {

    @Mapping(source = "processing.id", target = "processingId")
    @Mapping(source = "mark.id", target = "markId")
    MarkSuggestionDto toDto(MarkSuggestion suggestion);
}
