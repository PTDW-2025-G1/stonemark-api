package pt.estga.file.services.storage.variant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.file.services.storage.FileStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Handles storage path generation and saving variant content.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VariantStorageService {

    private final FileStorageService fileStorageService;
    private final StoragePathStrategy storagePathStrategy;

    public String storeVariant(MediaFile mediaFile, VariantResult variantResult, MediaVariantType type) throws IOException {
        String prefixPath = storagePathStrategy.generatePath(mediaFile);
        String variantPath = String.format("%s/derived/%s.webp", prefixPath, type.name().toLowerCase());
        try (InputStream is = Files.newInputStream(variantResult.file())) {
            return fileStorageService.storeFile(is, variantPath, variantResult.size());
        }
    }
}
