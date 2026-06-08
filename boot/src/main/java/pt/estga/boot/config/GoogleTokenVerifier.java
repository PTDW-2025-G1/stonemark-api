package pt.estga.boot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final RestTemplate restTemplate;

    public Map<String, Object> verify(String idToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = restTemplate.getForObject(TOKENINFO_URL + idToken, Map.class);
            if (payload == null || payload.containsKey("error")) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }
            return payload;
        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Google token verification failed", e);
        }
    }
}
