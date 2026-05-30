package pt.estga.mark.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkRequestDto;
import pt.estga.mark.entities.Mark;

@Mapper(componentModel = "spring")
public interface MarkMapper {

    MarkDto toDto(Mark mark);

    Mark toEntity(MarkRequestDto markDto);

    void update(Mark source, @MappingTarget Mark target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(MarkRequestDto dto, @MappingTarget Mark entity);
}
