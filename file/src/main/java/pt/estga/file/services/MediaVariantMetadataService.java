package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaVariant;
import pt.estga.file.enums.MediaVariantType;
import pt.estga.file.repositories.MediaVariantRepository;
import pt.estga.shared.exceptions.FileNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaVariantMetadataService {

    private final MediaVariantRepository mediaVariantRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "mediaVariantPaths", key = "#mediaId + '-' + #type")
    public String findVariantPath(Long mediaId, MediaVariantType type) {
        log.info("Fetching variant path from DB for media ID: {} type: {}", mediaId, type);
        return mediaVariantRepository.findByMediaFileIdAndType(mediaId, type)
                .map(MediaVariant::getStoragePath)
                .orElseThrow(() -> new FileNotFoundException("Variant not found: " + type + " for media " + mediaId));
    }
}
