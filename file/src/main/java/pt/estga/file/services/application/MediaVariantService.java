package pt.estga.file.services.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.repositories.MediaVariantRepository;
import pt.estga.file.services.storage.FileStorageService;
import pt.estga.sharedweb.exceptions.FileNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaVariantService {

    private final FileStorageService fileStorageService;
    private final MediaVariantRepository mediaVariantRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "mediaVariantPaths", key = "#mediaId + '-' + #type")
    public Resource loadVariant(Long mediaId, MediaVariantType type) {
        String storagePath = findVariantPath(mediaId, type);
        return fileStorageService.loadFile(storagePath);
    }

    private String findVariantPath(Long mediaId, MediaVariantType type) {
        log.info("Fetching variant path from DB for media ID: {} type: {}", mediaId, type);
        return mediaVariantRepository.findByMediaFileIdAndType(mediaId, type)
                .map(MediaVariant::getStoragePath)
                .orElseThrow(() -> new FileNotFoundException(
                        "Variant not found: " + type + " for media " + mediaId
                ));
    }
}
