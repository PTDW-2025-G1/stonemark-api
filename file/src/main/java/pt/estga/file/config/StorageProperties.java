package pt.estga.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized storage configuration. Replaces scattered @Value injections in services.
 */
@Getter
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** Provider name, e.g. local or s3. */
    @Setter

    private String provider = "local";

    /** Allowed MIME types for uploads. Externalized so it can be changed without code edits. */
    @Setter
    private List<String> allowedMimeTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    ));

    /** Maximum upload size in bytes. Files larger than this will be rejected. Default: 10 MiB */
    @Setter
    private long maxUploadSize = 10 * 1024 * 1024;

    /** Custom temp directory for upload and processing temp files. Falls back to system default if blank. */
    @Setter
    private String tempDir = "";

    /** Directory for staged files awaiting final commit. Relative to tempDir if not absolute. */
    @Setter
    private String stagingDir = "staging";

    private final Local local = new Local();

    @Setter
    @Getter
    public static class Local {
        /** Root path for local filesystem storage. */
        private String rootPath = "uploads";
    }
}

