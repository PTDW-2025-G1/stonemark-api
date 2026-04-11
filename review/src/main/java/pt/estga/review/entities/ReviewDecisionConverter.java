package pt.estga.review.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pt.estga.review.enums.ReviewDecision;

@Converter(autoApply = true)
public class ReviewDecisionConverter implements AttributeConverter<ReviewDecision, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ReviewDecision attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public ReviewDecision convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ReviewDecision.fromCode(dbData);
    }
}
