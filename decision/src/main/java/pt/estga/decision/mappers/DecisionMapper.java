package pt.estga.decision.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.estga.decision.dtos.ActiveDecisionViewDto;
import pt.estga.decision.entities.SubmissionDecisionAttempt;

@Mapper(componentModel = "spring")
public interface DecisionMapper {

    @Mapping(target = "decidedByUsername", source = "decidedBy.username")
    ActiveDecisionViewDto toActiveDecisionViewDto(SubmissionDecisionAttempt decision);
}
