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
import java.util.Collections;

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
        // Read the raw request body into memory once and create a cached wrapper to allow
        // downstream consumers (like Spring's @RequestBody) to read it again.
        byte[] bodyBytes;
        try (var is = request.getInputStream()) {
            bodyBytes = is.readAllBytes();
        }
        var wrapped = new CachedBodyHttpServletRequest(request, bodyBytes);
        int status = 0;
        try {
            log.debug("Incoming request {} {} to webhook path {}", request.getMethod(), request.getRequestURI(), webhookPath);
            // Log headers of interest
            Collections.list(request.getHeaderNames()).forEach(name -> log.debug("Header: {}={} ", name, request.getHeader(name)));

            // Convert body bytes to string payload and attach it to the wrapped request so downstream
            // handlers (controller) can access the raw JSON if needed for debugging.
            String payload = bodyBytes.length > 0 ? new String(bodyBytes, StandardCharsets.UTF_8) : null;
            if (payload != null && !payload.isBlank()) {
                log.debug("Raw webhook payload available and attached to request for downstream processing");
            }

            // Attach raw payload to wrapped request before dispatch so controller can access it.
            wrapped.setAttribute("RAW_WEBHOOK_BODY", payload);

            // If this payload contains a callback_query, attempt to parse directly and
            // dispatch to the bot synchronously to ensure prompt ack and processing.
            if (payload != null && payload.contains("\"callback_query\"")) {
                try {
                    var update = objectMapper.readValue(payload, org.telegram.telegrambots.meta.api.objects.Update.class);
                    if (update != null) {
                        try {
                            telegramBot.onWebhookUpdateReceived(update);
                            wrapped.setAttribute("BOT_DISPATCHED", Boolean.TRUE);
                            status = HttpServletResponse.SC_OK;
                            log.debug("Filter handled callback_query and dispatched to bot");
                            return;
                        } catch (Exception e) {
                            log.error("Filter dispatch error invoking bot", e);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Filter: failed to parse payload as Update, delegating to chain", e);
                }
            }

            // Otherwise, continue the filter chain so controller handles the request.
            try {
                filterChain.doFilter(wrapped, response);
            } catch (Throwable t) {
                log.error("Exception while dispatching Telegram webhook request to servlet", t);
                throw t;
            }

            status = response.getStatus();
            if (status >= 400) {
                log.warn("Telegram webhook response status: {} for {}", status, request.getRequestURI());
            } else {
                log.debug("Telegram webhook response status: {} for {}", status, request.getRequestURI());
            }
        } finally {
            // After chain, log request body (if any) and attach it to the request for downstream handlers.
            String payload;
            if (bodyBytes.length > 0) {
                payload = new String(bodyBytes, StandardCharsets.UTF_8);
                log.debug("Telegram webhook request body length: {}", bodyBytes.length);
            } else {
                payload = null;
                log.debug("Telegram webhook request body: <empty>");
            }

            // Check whether controller set BOT_DISPATCHED attribute on the wrapped request.
            Object dispatched = wrapped.getAttribute("BOT_DISPATCHED");
            log.debug("BOT_DISPATCHED attribute after dispatch: {}", dispatched);

            // Controlled fallback: if controller did not handle the request (non-200) and
            // the payload contains a callback_query, parse and dispatch to the bot so
            // the callback is acknowledged and processed. Avoid double-dispatch by
            // checking BOT_DISPATCHED attribute.
            if (status != 200 && payload != null && payload.contains("\"callback_query\"") && wrapped.getAttribute("BOT_DISPATCHED") == null) {
                try {
                    log.debug("Controller returned {} for webhook; attempting fallback dispatch for callback_query", status);
                    var parsed = objectMapper.readValue(payload, com.fasterxml.jackson.databind.JsonNode.class);
                    // Basic sanity check for presence of callback_query node
                    if (parsed != null && parsed.has("callback_query")) {
                        try {
                            // Map to Update so existing bot logic can process it
                            var update = objectMapper.treeToValue(parsed, org.telegram.telegrambots.meta.api.objects.Update.class);
                            telegramBot.onWebhookUpdateReceived(update);
                            wrapped.setAttribute("BOT_DISPATCHED", Boolean.TRUE);
                            log.debug("Fallback dispatch: delivered Update to bot");
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
