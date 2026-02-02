package pt.estga.file.services;

import pt.estga.file.enums.MediaVariantType;

/**
 * Service interface for managing the metadata of media variants.
 * Handles database operations and caching for MediaVariant entities.
 */
public interface MediaVariantMetadataService {

    /**
     * Finds the storage path for a specific media variant.
     * This method is cached to reduce database lookups.
     *
     * @param mediaId the ID of the media file
     * @param type    the type of variant
     * @return the storage path of the variant
     * @throws pt.estga.shared.exceptions.FileNotFoundException if the variant is not found
     */
    String findVariantPath(Long mediaId, MediaVariantType type);
}
