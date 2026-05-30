package pt.estga.file.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import pt.estga.file.config.UploadRateLimiter;

@Component
@RequiredArgsConstructor
public class UploadRateLimitInterceptor implements HandlerInterceptor {

    private final UploadRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        boolean isUpload = method.getBeanType() == MediaController.class
                && "uploadMedia".equals(method.getMethod().getName());
        if (!isUpload) {
            return true;
        }

        response.setHeader(UploadRateLimiter.RATE_LIMIT_REMAINING,
                String.valueOf(rateLimiter.getRemainingRequests(request)));

        if (!rateLimiter.isAllowed(request)) {
            long retryAfter = rateLimiter.getRetryAfterMillis(request);
            response.setHeader("Retry-After", String.valueOf(retryAfter / 1000));
            response.setHeader(UploadRateLimiter.RATE_LIMIT_RESET,
                    String.valueOf(System.currentTimeMillis() + retryAfter));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }

        return true;
    }
}
