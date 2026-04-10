package pt.estga.intake.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.intake.entities.MarkEvidenceSubmission;

@Mapper(componentModel = "spring")
public interface MarkEvidenceSubmissionMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromSubmission(MarkEvidenceSubmission source, @MappingTarget MarkEvidenceSubmission target);

}
