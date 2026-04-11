package pt.estga.review.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.estga.review.dtos.ReviewResultDto;
import pt.estga.review.entities.MarkEvidenceReview;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "submissionId", source = "submission.id")
    @Mapping(target = "selectedMarkId", source = "selectedMark.id")
    @Mapping(target = "decision", expression = "java(review.getDecision())")
    @Mapping(target = "comment", source = "comment")
    ReviewResultDto toDto(MarkEvidenceReview review);
}
