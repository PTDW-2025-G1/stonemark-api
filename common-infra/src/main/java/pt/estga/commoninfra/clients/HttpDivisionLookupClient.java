package pt.estga.commoninfra.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.estga.commoncore.interfaces.DivisionLookupClient;
import pt.estga.commoncore.models.DivisionRef;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Calls the external geo service to resolve administrative divisions.
 * Network or parsing failures are logged and degrade to empty results so a
 * temporarily unavailable geo service never breaks the calling request.
 */
@Slf4j
@Component
public class HttpDivisionLookupClient implements DivisionLookupClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public HttpDivisionLookupClient(
            @Value("${geo.api.base-url:http://localhost:8081}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Optional<DivisionRef> findByCode(String code) {
        String url = baseUrl + "/api/v1/geo-units/" + code;
        var request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return Optional.empty();
            var dto = objectMapper.readValue(response.body().string(), GeoUnitDto.class);
            return toDivisionRef(dto);
        } catch (IOException e) {
            log.warn("Division lookup failed for code '{}': {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, DivisionRef> findByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) return Map.of();
        String url = baseUrl + "/api/v1/geo-units/batch";
        try {
            String json = objectMapper.writeValueAsString(new BatchRequest(List.copyOf(codes)));
            var body = RequestBody.create(json, JSON);
            var request = new Request.Builder().url(url).post(body).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return Map.of();
                var results = objectMapper.readValue(response.body().string(), BatchResult[].class);
                return Arrays.stream(results)
                        .filter(r -> r.found && r.unit != null && r.unit.code != null)
                        .collect(Collectors.toMap(
                                r -> r.unit.code,
                                r -> new DivisionRef(r.unit.code, r.unit.name),
                                (a, b) -> a));
            }
        } catch (IOException e) {
            log.warn("Batch division lookup failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public Optional<DivisionRef> resolveByCoordinates(double latitude, double longitude) {
        String url = String.format("%s/api/v1/geo/reverse-geocode?lat=%s&lon=%s", baseUrl, latitude, longitude);
        var request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return Optional.empty();
            var dto = objectMapper.readValue(response.body().string(), GeoUnitDto.class);
            return toDivisionRef(dto);
        } catch (IOException e) {
            log.warn("Reverse geocode failed for ({}, {}): {}", latitude, longitude, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<DivisionRef> toDivisionRef(GeoUnitDto dto) {
        if (dto == null || dto.code == null) return Optional.empty();
        return Optional.of(new DivisionRef(dto.code, dto.name));
    }

    // Jackson deserialization targets — only the fields the client needs

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoUnitDto(String code, String name) {}

    private record BatchRequest(List<String> codes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchResult(String code, boolean found, GeoUnitDto unit) {}
}
