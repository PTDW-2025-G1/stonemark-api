package pt.estga.vision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisionClient {

    private final RestTemplate restTemplate;

    @Value("${vision.server.url}")
    private String detectionServerUrl;

    /**
     * Analyzes the provided image data to verify its content and extract a feature embedding.
     *
     * @param imageInputStream The InputStream of the image to analyze.
     * @param originalFilename The original filename of the image.
     * @return A {@link DetectionResult} containing the outcome of the analysis.
     */
    public DetectionResult detect(InputStream imageInputStream, String originalFilename) {
        log.info("Starting detection process for file: {}", originalFilename);

        // Determine the MediaType based on the filename
        MediaType fileMediaType = getMediaType(originalFilename);
        log.debug("Determined MediaType for file {}: {}", originalFilename, fileMediaType);

        // Use MultipartBodyBuilder to construct the request body
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new InputStreamResource(imageInputStream))
               .filename(originalFilename != null ? originalFilename : "image.bin") // Ensure filename is not null
               .contentType(fileMediaType);

        // Build the HttpEntity for the request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity =
                new HttpEntity<>(builder.build(), headers);

        log.debug("Sending request to detection server at: {}", detectionServerUrl + "/process");

        // Use DetectionResult directly for the response
        ResponseEntity<DetectionResult> response = restTemplate.postForEntity(detectionServerUrl + "/process", requestEntity, DetectionResult.class);

        DetectionResult result = response.getBody();

        if (result == null) {
            log.warn("Detection response body is null. Returning a failed detection result.");
            return new DetectionResult(false, new float[0]);
        }

        log.info("Detection process completed. Mason mark detected: {}", result.isMasonMark());
        return result;
    }

    /**
     * Lightweight health check against the vision server. Returns true when the
     * configured health endpoint responds with a 2xx status.
     */
    public boolean isAvailable() {
        if (detectionServerUrl == null || detectionServerUrl.isBlank()) {
            log.debug("Vision server url not configured - treating as unavailable");
            return false;
        }
        try {
            ResponseEntity<Void> resp = restTemplate.getForEntity(detectionServerUrl + "/health", Void.class);
            return resp.getStatusCode() != null && resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Vision health check failed", e);
            return false;
        }
    }

    @NonNull
    private static MediaType getMediaType(String originalFilename) {
        MediaType fileMediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (originalFilename != null) {
            String lowerCaseFilename = originalFilename.toLowerCase();
            if (lowerCaseFilename.endsWith(".jpg") || lowerCaseFilename.endsWith(".jpeg")) {
                fileMediaType = MediaType.IMAGE_JPEG;
            } else if (lowerCaseFilename.endsWith(".png")) {
                fileMediaType = MediaType.IMAGE_PNG;
            }
            // Other image types later
        }
        return fileMediaType;
    }
}
