package pt.estga.chatbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.context.ApplicationContext;

/**
 * Ensures the DispatcherServlet is initialized at application startup to avoid
 * first-request lazy initialization delays which can cause Telegram callback timeouts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DispatcherWarmup implements CommandLineRunner {

    private final ApplicationContext context;

    @Override
    public void run(String... args) {
        try {
            // Accessing the DispatcherServlet bean forces initialization.
            DispatcherServlet servlet = context.getBean(DispatcherServlet.class);
            log.info("DispatcherServlet bean obtained at startup: {}", servlet.getServletName());
        } catch (Exception e) {
            log.warn("DispatcherServlet warmup failed or not present: {}", e.getMessage());
        }
    }
}
