package pt.estga.chatbot.features.auth;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.config.ChatbotAuthProperties;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.services.AuthService;
import pt.estga.chatbot.services.AuthServiceFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AuthenticationGuard {

    private final AuthServiceFactory authServiceFactory;
    private final Map<ConversationState, ConversationStateHandler> handlers;
    private final ChatbotAuthProperties chatbotAuthProperties;

    public AuthenticationGuard(
            AuthServiceFactory authServiceFactory,
            ChatbotAuthProperties chatbotAuthProperties,
            List<ConversationStateHandler> handlerList
    ) {
        this.authServiceFactory = authServiceFactory;
        this.chatbotAuthProperties = chatbotAuthProperties;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(ConversationStateHandler::canHandle, Function.identity()));
    }

    public boolean isActionAllowed(BotInput input, ConversationState currentState) {
        // Global switch: when chatbot auth is optional, allow whole conversation flow.
        if (chatbotAuthProperties.isOptional()) {
            return true;
        }

        // If the user is authenticated, always allow the action.
        if (isAuthenticated(input)) {
            return true;
        }

        // If the user is not authenticated, check if the handler for the current state allows unauthenticated access.
        if (currentState != null) {
            ConversationStateHandler handler = handlers.get(currentState);
            if (handler != null) {
                RequiresAuthentication annotation = handler.getClass().getAnnotation(RequiresAuthentication.class);
                // If the annotation is present and its value is false, access is allowed.
                return annotation != null && !annotation.value();
            }
        }

        // By default, deny access.
        return false;
    }

    private boolean isAuthenticated(BotInput input) {
        if (input == null || input.getPlatform() == null || input.getUserId() == null) {
            return false;
        }
        AuthService authService = authServiceFactory.getAuthService(input.getPlatform());
        return authService.isAuthenticated(input.getUserId());
    }
}
