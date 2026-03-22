package pt.estga.shared.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Minimal replacement for Spring Boot's MailProperties to avoid coupling with
 * spring-boot-autoconfigure. Supports host, port, username, password and extra properties.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "spring.mail")
public class AppMailProperties {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private Map<String, String> properties = new HashMap<>();

}

