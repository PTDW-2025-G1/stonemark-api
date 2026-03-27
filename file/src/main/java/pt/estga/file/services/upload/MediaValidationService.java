package pt.estga.file.services.upload;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Validates media files (mime type) using Apache Tika. Keeps validation logic isolated.
 */
@Service
@Slf4j
public class MediaValidationService {

    private final Tika tika = new Tika();

    /**
     * Detects mime type of the file and returns true if it is contained in allowedMimeTypes.
     */
    public boolean isAllowedImage(Path file, Set<String> allowedMimeTypes) throws IOException {
        String mime = tika.detect(file);
        log.debug("Detected MIME type {} for file {}", mime, file);
        return mime != null && allowedMimeTypes.contains(mime);
    }
}

