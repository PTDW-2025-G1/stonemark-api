package pt.estga.file.services.naming;

import org.springframework.stereotype.Component;
import pt.estga.file.entities.MediaFile;

/**
 * Generates storage paths based on the stored filename UUID to spread files
 * across directories and avoid hot spots. Assumes filenames are UUID-based
 * optionally with an extension. Structure: {prefix}/{filename}
 * where prefix are the first two hex chars of the UUID.
 */
@Component
public class StoragePathStrategy {

    public String generatePath(MediaFile mediaFile) {
        if (mediaFile.getFilename() == null) {
            throw new IllegalArgumentException("MediaFile filename cannot be null");
        }

        String filename = mediaFile.getFilename().replace("\\", "/");

        // Extract UUID prefix (first four chars) and split into two levels: ab/cd/filename
        String base = filename;
        int dot = filename.indexOf('.');
        if (dot > 0) base = filename.substring(0, dot);

        String part = base.length() >= 4 ? base.substring(0,4) : String.format("%4s", base).replace(' ', 'x');
        String p1 = part.substring(0,2);
        String p2 = part.substring(2,4);

        return String.format("%s/%s/%s", p1, p2, filename);
    }
}
