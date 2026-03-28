package pt.estga.chatbot.features.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.config.ChatbotAuthProperties;
import pt.estga.chatbot.constants.EmojiKey;
import pt.estga.chatbot.features.submission.SubmissionCallbackData;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.ui.Button;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.features.verification.VerificationCallbackData;
import pt.estga.chatbot.services.AuthService;
import pt.estga.chatbot.services.AuthServiceFactory;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MainMenuFactory {

    private final AuthServiceFactory authServiceFactory;
    private final UiTextService textService;
    private final ChatbotAuthProperties chatbotAuthProperties;

    public Menu create(BotInput input) {
        AuthService authService = authServiceFactory.getAuthService(input.getPlatform());
        boolean isAuthenticated = authService.isAuthenticated(input.getUserId());

        List<List<Button>> buttonRows = new ArrayList<>();
        boolean canStartProposal = chatbotAuthProperties.isOptional() || isAuthenticated;

        if (canStartProposal) {
            buttonRows.add(List.of(
                    Button.builder()
                            .textNode(textService.get(MessageKey.PROPOSE_MARK_BTN))
                            .callbackData(SubmissionCallbackData.START_SUBMISSION)
                            .build()
            ));
        }

        if (!isAuthenticated) {
            buttonRows.add(List.of(
                    Button.builder()
                            .textNode(textService.get(MessageKey.CONNECT_ACCOUNT_BTN, EmojiKey.KEY))
                            .callbackData(VerificationCallbackData.START_VERIFICATION)
                            .build()
            ));
        }

        return Menu.builder()
                .titleNode(textService.get(MessageKey.HELP_OPTIONS_TITLE))
                .buttons(buttonRows)
                .build();
    }
}
