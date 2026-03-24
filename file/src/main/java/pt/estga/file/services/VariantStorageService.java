package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.models.VariantResult;

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

    public String storeVariant(MediaFile mediaFile, VariantResult variantResult, MediaVariantType type) throws IOException {
        // Use the stored filename as base so derived variants are colocated with
        // original content and avoid reliance on database id for pathing.
        String baseName = mediaFile.getFilename();
        String variantPath = String.format("derived/%s/%s.webp", baseName, type.name().toLowerCase());
        try (InputStream is = Files.newInputStream(variantResult.file())) {
            var res = mediaContentService.saveContent(is, variantPath);
            return res.storagePath();
        }
    }
}

