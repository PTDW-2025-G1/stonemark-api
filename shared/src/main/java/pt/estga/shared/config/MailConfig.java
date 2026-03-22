package pt.estga.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configuration to expose a configured JavaMailSender bean.
 * Uses a local configuration properties class to avoid depending on the
 * Spring Boot autoconfigure mail package directly.
 */
@Configuration
@EnableConfigurationProperties(AppMailProperties.class)
@RequiredArgsConstructor
public class MailConfig {

    private final AppMailProperties mailProperties;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(mailProperties.getPassword());

        if (mailProperties.getProperties() != null && !mailProperties.getProperties().isEmpty()) {
            mailSender.getJavaMailProperties().putAll(mailProperties.getProperties());
        }

        return mailSender;
    }
}
