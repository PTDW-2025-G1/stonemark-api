package pt.estga.file.services;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Service
@Slf4j
public class ImageVariantGenerator {

    private final StorageProperties storageProperties;

    public ImageVariantGenerator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public VariantResult generate(Path originalFile, MediaVariantType type) throws IOException {
        Path temp = createTempFile("variant-", ".webp");

        try {
            Thumbnails.Builder<File> builder = Thumbnails.of(originalFile.toFile());
            switch (type) {
                case THUMBNAIL -> builder.size(200, 200).crop(net.coobird.thumbnailator.geometry.Positions.CENTER);
                case PREVIEW -> builder.size(1024, 1024);
                case OPTIMIZED -> builder.scale(1.0);
            }
            builder.outputFormat("webp").toFile(temp.toFile());

            int width = 0;
            int height = 0;
            try (ImageInputStream iis = ImageIO.createImageInputStream(temp.toFile())) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader r = readers.next();
                    try {
                        r.setInput(iis);
                        width = r.getWidth(0);
                        height = r.getHeight(0);
                    } finally {
                        r.dispose();
                    }
                }
            }

            long size = Files.size(temp);
            return new VariantResult(temp, width, height, size);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    private Path createTempFile(String prefix, String suffix) throws IOException {
        String customDir = storageProperties.getTempDir();
        if (StringUtils.hasText(customDir)) {
            Path dir = Path.of(customDir);
            Files.createDirectories(dir);
            return Files.createTempFile(dir, prefix, suffix);
        }
        return Files.createTempFile(prefix, suffix);
    }
}

