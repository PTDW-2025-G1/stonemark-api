package pt.estga.review.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.estga.review.dtos.ReviewResponseDto;
import pt.estga.review.entities.MarkEvidenceReview;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "submission.id", target = "submissionId")
    @Mapping(source = "selectedMark.id", target = "selectedMarkId")
    @Mapping(source = "selectedMark.title", target = "selectedMarkTitle")
    @Mapping(source = "reviewedBy.id", target = "reviewerId")
    @Mapping(expression = "java(review.getReviewedBy() != null ? review.getReviewedBy().getFirstName() + \" \" + review.getReviewedBy().getLastName() : \"System\")", target = "reviewerName")
    ReviewResponseDto toDto(MarkEvidenceReview review);
}