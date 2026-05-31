package pt.estga.file.services;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pt.estga.file.config.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TempFileFactory {

    private final StorageProperties storageProperties;

    public TempFileFactory(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public Path createTempFile(String prefix, String suffix) throws IOException {
        String customDir = storageProperties.getTempDir();
        if (StringUtils.hasText(customDir)) {
            Path dir = Path.of(customDir);
            Files.createDirectories(dir);
            return Files.createTempFile(dir, prefix, suffix);
        }
        return Files.createTempFile(prefix, suffix);
    }
}
