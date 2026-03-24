package pt.estga.boot.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Component("vision")
public class VisionHealthIndicator implements HealthIndicator {

    @Value("${vision.server.url:}")
    private String visionUrl;

    @Override
    public Health health() {
        if (visionUrl == null || visionUrl.isBlank()) {
            return Health.down().withDetail("error", "vision.server.url not configured").build();
        }

        try {
            URI uri = URI.create(visionUrl);
            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            int code = con.getResponseCode();
            if (code >= 200 && code < 300) {
                return Health.up().withDetail("url", visionUrl).withDetail("status", code).build();
            }
            return Health.down().withDetail("url", visionUrl).withDetail("status", code).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("url", visionUrl).build();
        }
    }
}


