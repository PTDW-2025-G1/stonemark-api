package pt.estga.chatbot.features.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.config.ChatbotAuthProperties;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.models.ui.Button;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.services.AuthService;
import pt.estga.chatbot.services.messages.UiTextService;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class MainMenuFactory {

    private final List<AuthService> authServices;
    private final UiTextService textService;
    private final ChatbotAuthProperties chatbotAuthProperties;

    public Menu create(BotInput input) {
        AuthService authService = resolveAuthService(input.getPlatform());
        boolean isAuthenticated = authService.isAuthenticated(input.getUserId());

        List<List<Button>> buttonRows = new ArrayList<>();
        boolean canStartSubmission = chatbotAuthProperties.isOptional() || isAuthenticated;

        if (canStartSubmission) {
            buttonRows.add(List.of(
                    Button.builder()
                            .textNode(textService.get(MessageKey.PROPOSE_MARK_BTN))
                            .callbackData(CallbackData.START_SUBMISSION)
                            .build()
            ));
        }

        if (!isAuthenticated) {
            buttonRows.add(List.of(
                    Button.builder()
                            .textNode(textService.get(MessageKey.CONNECT_ACCOUNT_BTN))
                            .callbackData(CallbackData.START_VERIFICATION)
                            .build()
            ));
        }

        return Menu.builder()
                .titleNode(textService.get(MessageKey.HELP_OPTIONS_TITLE))
                .buttons(buttonRows)
                .build();
    }

    private AuthService resolveAuthService(Platform platform) {
        return authServices.stream()
                .filter(s -> s.supports(platform))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No AuthService found for platform: " + platform));
    }
}
