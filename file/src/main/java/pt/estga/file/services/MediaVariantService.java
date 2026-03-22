package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.enums.MediaVariantType;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaVariantService {

    private final MediaVariantMetadataService mediaVariantMetadataService;
    private final FileStorageService fileStorageService;

    public Resource loadVariant(Long mediaId, MediaVariantType type) {
        // Delegate metadata lookup to a separate service to ensure AOP proxies work for @Cacheable
        String storagePath = mediaVariantMetadataService.findVariantPath(mediaId, type);
        return fileStorageService.loadFile(storagePath);
    }
}
