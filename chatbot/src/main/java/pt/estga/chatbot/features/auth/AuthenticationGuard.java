package pt.estga.chatbot.features.auth;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.config.ChatbotAuthProperties;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.services.AuthService;
import pt.estga.chatbot.services.AuthServiceFactory;

@Component
public class AuthenticationGuard {

    private final AuthServiceFactory authServiceFactory;
    private final ChatbotAuthProperties chatbotAuthProperties;

    public AuthenticationGuard(
            AuthServiceFactory authServiceFactory,
            ChatbotAuthProperties chatbotAuthProperties
    ) {
        this.authServiceFactory = authServiceFactory;
        this.chatbotAuthProperties = chatbotAuthProperties;
    }

    public boolean isActionAllowed(BotInput input) {
        // Global switch: when chatbot auth is optional, allow whole conversation flow.
        if (chatbotAuthProperties.isOptional()) {
            return true;
        }

        // If authentication is required, check if the user is authenticated.
        return isAuthenticated(input);
    }

    private boolean isAuthenticated(BotInput input) {
        if (input == null || input.getPlatform() == null || input.getUserId() == null) {
            return false;
        }
        AuthService authService = authServiceFactory.getAuthService(input.getPlatform());
        return authService.isAuthenticated(input.getUserId());
    }
}
