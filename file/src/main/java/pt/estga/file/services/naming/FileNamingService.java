package pt.estga.file.services.naming;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pt.estga.file.entities.MediaFile;

/**
 * Generates stored filenames and storage paths for media files.
 */
@Service
public class FileNamingService {

    public String generateStoredFilename(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uuid = java.util.UUID.randomUUID().toString();
        return uuid + (extension != null ? "." + extension : "");
    }

    public String generatePath(MediaFile mediaFile) {
        return generatePath(mediaFile.getFilename());
    }

    public String generatePath(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }

        filename = filename.replace("\\", "/");

        String base = filename;
        int dot = filename.indexOf('.');
        if (dot > 0) base = filename.substring(0, dot);

        String part = base.length() >= 4 ? base.substring(0,4) : String.format("%4s", base).replace(' ', 'x');
        String p1 = part.substring(0,2);
        String p2 = part.substring(2,4);

        return String.format("%s/%s/%s", p1, p2, filename);
    }
}

