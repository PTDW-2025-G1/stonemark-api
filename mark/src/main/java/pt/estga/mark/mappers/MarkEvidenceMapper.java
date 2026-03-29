package pt.estga.mark.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkEvidenceRequestDto;
import pt.estga.mark.entities.MarkEvidence;

@Mapper(componentModel = "spring", uses = {MarkMapper.class})
public interface MarkEvidenceMapper {

    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "occurrenceId", source = "occurrence.id")
    @Mapping(target = "markId", source = "occurrence.mark.id")
    MarkEvidenceDto toDto(MarkEvidence evidence);

    @Mapping(target = "file", ignore = true)
    @Mapping(target = "occurrence", ignore = true)
    @Mapping(target = "embedding", source = "embedding")
    MarkEvidence toEntity(MarkEvidenceDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "occurrence", ignore = true)
    void updateEntityFromDto(MarkEvidenceDto dto, @MappingTarget MarkEvidence entity);

    @Mapping(target = "file", ignore = true)
    @Mapping(target = "occurrence", ignore = true)
    @Mapping(target = "embedding", source = "embedding")
    MarkEvidence toEntityFromRequest(MarkEvidenceRequestDto dto);

    @Mapping(target = "file", ignore = true)
    @Mapping(target = "occurrence", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(MarkEvidenceRequestDto dto, @MappingTarget MarkEvidence entity);
}
