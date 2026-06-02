package pt.estga.monument.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pt.estga.monument.enums.MonumentValidationState;

/**
 * Persists {@link MonumentValidationState} as its ordinal integer value.
 */
@Converter
public class MonumentValidationStateConverter implements AttributeConverter<MonumentValidationState, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MonumentValidationState attribute) {
        return attribute == null ? null : attribute.ordinal();
    }

    @Override
    public MonumentValidationState convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        MonumentValidationState[] vals = MonumentValidationState.values();
        if (dbData < 0 || dbData >= vals.length) return null;
        return vals[dbData];
    }
}
