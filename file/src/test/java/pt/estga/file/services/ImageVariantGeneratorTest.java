package pt.estga.file.services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageVariantGeneratorTest {

    private final ImageVariantGenerator generator = new ImageVariantGenerator(new StorageProperties());

    @TempDir
    static Path tempDir;

    static Path testImage;

    @BeforeAll
    static void createTestImage() throws Exception {
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.drawRect(0, 0, 399, 299);
        g.dispose();

        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        testImage = tempDir.resolve("test.jpg");
        Files.write(testImage, baos.toByteArray());
    }

    @Test
    @DisplayName("should generate the single optimized variant with the original dimensions")
    void shouldGenerateOptimized() throws Exception {
        VariantResult result = generator.generate(testImage, MediaVariantType.OPTIMIZED);

        try {
            assertEquals(400, result.width());
            assertEquals(300, result.height());
            assertTrue(result.size() > 0);
        } finally {
            Files.deleteIfExists(result.file());
        }
    }
}
