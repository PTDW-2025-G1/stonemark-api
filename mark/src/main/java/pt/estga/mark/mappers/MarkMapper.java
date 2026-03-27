package pt.estga.mark.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkRequestDto;
import pt.estga.mark.entities.Mark;
import pt.estga.file.mappers.MediaFileMapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaFileMapper.class})
public interface MarkMapper {

    MarkDto toDto(Mark mark);

    List<MarkDto> toDto(List<Mark> marks);

    Mark toEntity(MarkRequestDto markDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(MarkRequestDto dto, @MappingTarget Mark entity);

}
