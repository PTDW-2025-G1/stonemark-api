package pt.estga.processing.vision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Sends the image to the Vision server and returns its embedding with provenance.
     *
     * @param imageInputStream the image to analyze
     * @param originalFilename the original filename, used to pick the media type
     */
    public DetectionResult detectMark(InputStream imageInputStream, String originalFilename) {
        log.debug("Sending image {} to Vision server at {}", originalFilename, detectionServerUrl + "/process");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new InputStreamResource(imageInputStream))
                .filename(originalFilename != null ? originalFilename : "image.bin")
                .contentType(getMediaType(originalFilename));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity = new HttpEntity<>(builder.build(), headers);

        ResponseEntity<DetectionResult> response =
                restTemplate.postForEntity(detectionServerUrl + "/process", requestEntity, DetectionResult.class);

        DetectionResult result = response.getBody();
        if (result == null) {
            log.warn("Vision response body is null; returning an empty embedding");
            return new DetectionResult(new float[0], null, null, null);
        }
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
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Vision health check failed", e);
            return false;
        }
    }

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
