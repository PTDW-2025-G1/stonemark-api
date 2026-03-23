package pt.estga.file.services.naming;

import org.springframework.stereotype.Component;
import pt.estga.file.entities.MediaFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates storage paths based on the current date.
 * Structure: yyyy/MM/dd/{filename}
 * Example: 2023/10/27/stonemark-123.jpg
 */
@Component
public class StoragePathStrategy {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public String generatePath(MediaFile mediaFile) {
        if (mediaFile.getFilename() == null) {
            throw new IllegalArgumentException("MediaFile filename cannot be null");
        }

        LocalDate now = LocalDate.now(ZoneId.of("UTC"));
        String datePath = now.format(DATE_FORMATTER);
        
        // Ensure forward slashes for consistency across OS
        String normalizedFilename = mediaFile.getFilename().replace("\\", "/");

        return String.format("%s/%s", datePath, normalizedFilename);
    }
}
