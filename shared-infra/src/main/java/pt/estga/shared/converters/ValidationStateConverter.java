package pt.estga.shared.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pt.estga.shared.enums.ValidationState;

/**
 * Persists {@link ValidationState} as its ordinal integer value.
 */
@Converter
public class ValidationStateConverter implements AttributeConverter<ValidationState, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ValidationState attribute) {
        return attribute == null ? null : attribute.ordinal();
    }

    @Override
    public ValidationState convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        ValidationState[] vals = ValidationState.values();
        if (dbData < 0 || dbData >= vals.length) return null;
        return vals[dbData];
    }
}
