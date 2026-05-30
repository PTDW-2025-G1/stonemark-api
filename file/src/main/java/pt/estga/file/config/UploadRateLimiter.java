package pt.estga.file.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class UploadRateLimiter {

    private final Map<String, ConcurrentLinkedDeque<Long>> requests = new ConcurrentHashMap<>();

    private final boolean enabled;
    private final int maxRequests;
    private final long windowMillis;

    public UploadRateLimiter(
            @Value("${upload.rate-limit.enabled:true}") boolean enabled,
            @Value("${upload.rate-limit.max-requests:10}") int maxRequests,
            @Value("${upload.rate-limit.window-seconds:60}") long windowSeconds) {
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
    }

    public static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET = "X-RateLimit-Reset";

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAllowed(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String clientIp = resolveClientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        ConcurrentLinkedDeque<Long> timestamps = requests.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
        }
        return true;
    }

    public long getRetryAfterMillis(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        ConcurrentLinkedDeque<Long> timestamps = requests.get(clientIp);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }
        synchronized (timestamps) {
            Long oldest = timestamps.peekFirst();
            if (oldest == null) {
                return 0;
            }
            return Math.max(0, oldest + windowMillis - System.currentTimeMillis());
        }
    }

    public int getRemainingRequests(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        ConcurrentLinkedDeque<Long> timestamps = requests.get(clientIp);
        if (timestamps == null) {
            return maxRequests;
        }

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            return Math.max(0, maxRequests - timestamps.size());
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
