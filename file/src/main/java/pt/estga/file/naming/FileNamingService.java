package pt.estga.file.naming;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Generates stored filenames for media files. Keeps naming strategy centralized for reuse.
 */
@Service
public class FileNamingService {

    /**
     * Generates a stored filename based on a UUID. This avoids coupling filenames
     * to database-generated ids and produces safe, deterministic filenames.
     * The original filename's extension is preserved when present.
     *
     * @param originalFilename optional original filename used only to preserve extension
     * @return uuid-based filename (example: 3f9e7a1c-... .jpg)
     */
    public String generateStoredFilename(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uuid = java.util.UUID.randomUUID().toString();
        return uuid + (extension != null ? "." + extension : "");
    }
}

