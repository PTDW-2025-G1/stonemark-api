package pt.estga.mark.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pt.estga.mark.enums.MarkValidationState;

/**
 * Persists {@link MarkValidationState} as its ordinal integer value.
 */
@Converter
public class ValidationStateConverter implements AttributeConverter<MarkValidationState, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MarkValidationState attribute) {
        return attribute == null ? null : attribute.ordinal();
    }

    @Override
    public MarkValidationState convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        MarkValidationState[] vals = MarkValidationState.values();
        if (dbData < 0 || dbData >= vals.length) return null;
        return vals[dbData];
    }
}
