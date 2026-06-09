package pt.estga.chatbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.filter.OncePerRequestFilter;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Logs incoming HTTP requests for the Telegram webhook path so that webhook requests
 * (including raw JSON body) can be observed even if JSON mapping fails later.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TelegramWebhookRequestLoggingFilter extends OncePerRequestFilter {

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    private final ObjectMapper objectMapper;
    private final StonemarkTelegramBot telegramBot;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Only filter requests that target the configured webhook path.
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
        int status = 0;
        try {
            String payload = bodyBytes.length > 0 ? new String(bodyBytes, StandardCharsets.UTF_8) : null;
            if (payload != null && !payload.isBlank()) {
                wrapped.setAttribute("RAW_WEBHOOK_BODY", payload);
            }

            if (payload != null && payload.contains("\"callback_query\"")) {
                try {
                    var update = objectMapper.readValue(payload, org.telegram.telegrambots.meta.api.objects.Update.class);
                    if (update != null) {
                        try {
                            telegramBot.onWebhookUpdateReceived(update);
                            wrapped.setAttribute("BOT_DISPATCHED", Boolean.TRUE);
                            status = HttpServletResponse.SC_OK;
                            return;
                        } catch (Exception e) {
                            log.error("Filter dispatch error invoking bot", e);
                        }
                    }
                } catch (Exception e) {
                }
            }

            try {
                filterChain.doFilter(wrapped, response);
            } catch (Throwable t) {
                log.error("Exception while dispatching Telegram webhook request to servlet", t);
                throw t;
            }

            status = response.getStatus();
            if (status >= 400) {
                log.warn("Telegram webhook response status: {} for {}", status, request.getRequestURI());
            }
        } finally {
            String payload;
            if (bodyBytes.length > 0) {
                payload = new String(bodyBytes, StandardCharsets.UTF_8);
            } else {
                payload = null;
            }
            if (status != 200 && payload != null && payload.contains("\"callback_query\"") && wrapped.getAttribute("BOT_DISPATCHED") == null) {
                try {
                    var parsed = objectMapper.readValue(payload, com.fasterxml.jackson.databind.JsonNode.class);
                    if (parsed != null && parsed.has("callback_query")) {
                        try {
                            var update = objectMapper.treeToValue(parsed, org.telegram.telegrambots.meta.api.objects.Update.class);
                            telegramBot.onWebhookUpdateReceived(update);
                            wrapped.setAttribute("BOT_DISPATCHED", Boolean.TRUE);
                        } catch (Exception e) {
                            log.error("Fallback dispatch: error invoking bot", e);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Fallback dispatch: failed to parse payload", e);
                }
            }
        }
    }
}
