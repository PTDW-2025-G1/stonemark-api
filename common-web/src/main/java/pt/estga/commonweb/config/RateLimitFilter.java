package pt.estga.commonweb.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        Bucket bucket = resolveBucket(clientIp, path);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            if (retryAfterSeconds == 0) {
                retryAfterSeconds = 1;
            }
            log.warn("Rate limit exceeded for IP: {}, path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));
        }
    }

    private Bucket resolveBucket(String ip, String path) {
        String matchingPath = findMatchingPathConfig(path);
        String key = matchingPath != null ? ip + ":" + matchingPath : ip;
        return buckets.computeIfAbsent(key, k -> createBucket(matchingPath));
    }

    private String findMatchingPathConfig(String path) {
        Map<String, RateLimitProperties.BandwidthConfig> pathConfigs = properties.getPaths();
        if (pathConfigs != null) {
            for (String pattern : pathConfigs.keySet()) {
                if (pathMatcher.match(pattern, path)) {
                    return pattern;
                }
            }
        }
        return null;
    }

    private Bucket createBucket(String pathPattern) {
        RateLimitProperties.BandwidthConfig config;
        if (pathPattern != null && properties.getPaths() != null) {
            config = properties.getPaths().get(pathPattern);
        } else {
            config = properties.getDefaultLimit();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(config.getCapacity(),
                        Refill.greedy(config.getRefillTokens(),
                                Duration.of(config.getRefillPeriod(), config.getRefillUnit()))))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
