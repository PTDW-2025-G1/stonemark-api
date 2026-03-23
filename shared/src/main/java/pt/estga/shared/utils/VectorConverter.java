package pt.estga.shared.utils;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // This is the magic part
public class VectorConverter implements AttributeConverter<float[], Object> {

    @Override
    public Object convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        return new PGvector(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(Object dbData) {
        if (dbData == null) return null;
        // The driver returns a PGvector object
        if (dbData instanceof PGvector) {
            return ((PGvector) dbData).toArray();
        }
        return null;
    }
}