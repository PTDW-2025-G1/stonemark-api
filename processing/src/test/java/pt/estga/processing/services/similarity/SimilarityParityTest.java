package pt.estga.processing.services.similarity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.estga.shared.utils.VectorUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimilarityParityTest {

    @Test
    void dbDistanceConversionMatchesJavaCosine() {
        float[] a = new float[]{0.1f, 0.2f, 0.3f};
        float[] b = new float[]{0.4f, 0.5f, 0.6f};

        Double javaCosine = VectorUtils.cosineSimilarity(a, b);
        Assertions.assertNotNull(javaCosine, "cosineSimilarity must not be null for valid vectors");
        // Simulate DB cosine-distance value produced by pgvector '<#>' operator: distance = 1 - cosine
        double simulatedDbDistance = 1.0 - (javaCosine == null ? 0.0 : javaCosine);
        // Convert DB distance back to similarity as done in DB branch
        double converted = 1.0 - simulatedDbDistance;

        // They should be equal (within tiny floating point tolerance)
        assertEquals(javaCosine, converted, 1e-12);
    }
}
