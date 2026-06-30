package pt.estga.chatbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TelegramWebhookRequestLoggingFilter extends OncePerRequestFilter {

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.contains(webhookPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] bodyBytes;
        try (var is = request.getInputStream()) {
            bodyBytes = is.readAllBytes();
        }
        var wrapped = new CachedBodyHttpServletRequest(request, bodyBytes);

        if (bodyBytes.length > 0) {
            String payload = new String(bodyBytes, StandardCharsets.UTF_8);
            log.info("Telegram webhook payload: {}", payload);
        }

        filterChain.doFilter(wrapped, response);

        int status = response.getStatus();
        if (status >= 400) {
            log.warn("Telegram webhook response status: {} for {}", status, request.getRequestURI());
        }
    }
}
