package pt.estga.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chatbot.auth")
public class ChatbotAuthProperties {
    private boolean optional = true;
}
