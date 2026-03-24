package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;
import pt.estga.file.services.naming.StoragePathStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Handles storage path generation and saving variant content using existing MediaContentService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VariantStorageService {

    private final MediaContentService mediaContentService;
    private final StoragePathStrategy storagePathStrategy;

    public String storeVariant(MediaFile mediaFile, VariantResult variantResult, MediaVariantType type) throws IOException {
        // Place variants under the same sharded prefix as the original file to
        // avoid growing a single directory when many variants exist for a file.
        // Resulting structure: {p1}/{p2}/{filename}/derived/{type}.webp
        String prefixPath = storagePathStrategy.generatePath(mediaFile);
        // generatePath returns p1/p2/filename - append derived segment
        String variantPath = String.format("%s/derived/%s.webp", prefixPath, type.name().toLowerCase());
        try (InputStream is = Files.newInputStream(variantResult.file())) {
            var res = mediaContentService.saveContent(is, variantPath);
            return res.storagePath();
        }
    }
}

