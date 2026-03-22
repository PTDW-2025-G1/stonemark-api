package pt.estga.chatbot.features.verification.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.services.ChatbotVerificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateVerificationCodeHandler implements ConversationStateHandler {

    private final ChatbotVerificationService verificationService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String platformUserId = input.getUserId();

        // Generate code for this platform user
        ActionCode actionCode = verificationService.generateChatbotVerificationCode(platformUserId);

        // Store code in context to display
        context.setVerificationCode(actionCode.getCode());

        log.info("Generated verification code for platform user: {}", platformUserId);

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
}
