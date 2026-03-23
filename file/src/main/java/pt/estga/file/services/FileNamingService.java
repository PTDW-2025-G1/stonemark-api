package pt.estga.file.services;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pt.estga.file.entities.MediaFile;

/**
 * Generates stored filenames for media files. Keeps naming strategy centralized for reuse.
 */
@Service
public class FileNamingService {

    public String generateStoredFilename(MediaFile media, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return "stonemark-" + media.getId() + (extension != null ? "." + extension : "");
    }
}

