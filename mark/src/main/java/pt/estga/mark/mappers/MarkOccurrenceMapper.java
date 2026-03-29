package pt.estga.mark.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.mark.dtos.MarkOccurrenceRequestDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.monument.MonumentMapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MarkMapper.class, MonumentMapper.class})
public interface MarkOccurrenceMapper {

    @Mapping(target = "markId", source = "mark.id")
    @Mapping(target = "monumentId", source = "monument.id")
    MarkOccurrenceDto toDto(MarkOccurrence entity);

    @Mapping(source = "markId", target = "mark.id")
    @Mapping(source = "monumentId", target = "monument.id")
    @Mapping(target = "embedding", ignore = true)
    MarkOccurrence toEntity(MarkOccurrenceRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "markId", target = "mark.id")
    @Mapping(target = "embedding", ignore = true)
    void updateEntityFromDto(MarkOccurrenceRequestDto dto, @MappingTarget MarkOccurrence entity);
}
