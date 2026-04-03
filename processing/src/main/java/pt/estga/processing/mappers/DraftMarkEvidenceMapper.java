package pt.estga.processing.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.processing.entities.DraftMarkEvidence;

@Mapper(componentModel = "spring")
public interface DraftMarkEvidenceMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDraft(DraftMarkEvidence source, @MappingTarget DraftMarkEvidence target);

}
