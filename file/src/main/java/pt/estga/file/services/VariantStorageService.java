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
        String variantPath = String.format("%d/derived/%s.webp", mediaFile.getId(), type.name().toLowerCase());
        try (InputStream is = Files.newInputStream(variantResult.file())) {
            return mediaContentService.saveContent(is, variantPath);
        }
    }
}

