package pt.estga.chatbot.features.verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.services.messages.UiTextService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.services.ChatbotVerificationService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateVerificationCodeHandler implements ConversationStateHandler {

    private final ChatbotVerificationService verificationService;
    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String platformUserId = input.getUserId();

        ActionCode actionCode = verificationService.generateChatbotVerificationCode(platformUserId);

        context.setVerificationCode(actionCode.getCode());

        log.debug("Generated verification code for platform user: {}", platformUserId);

        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.DISPLAYING_VERIFICATION_CODE;
    }

    @Override
    public boolean isAutomatic() {
        return true;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome == HandlerOutcome.FAILURE) {
            return currentState;
        }
        return CoreState.START;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        List<BotResponse> responses = new ArrayList<>();
        RenderedText instructions = textService.get(MessageKey.CONNECT_MESSENGER_INSTRUCTIONS);
        responses.addAll(BotResponse.menuResponse(instructions));
        String code = context.getVerificationCode();
        responses.add(BotResponse.builder()
                .textNode(textService.get(MessageKey.CONNECT_MESSENGER_CODE, code))
                .build());
        responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
        return responses;
    }
}
