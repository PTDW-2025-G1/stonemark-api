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
import pt.estga.user.entities.User;
import pt.estga.user.services.UserIdentityService;
import pt.estga.verification.services.ChatbotVerificationService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitVerificationCodeHandler implements ConversationStateHandler {

    private final ChatbotVerificationService verificationService;
    private final UserIdentityService userIdentityService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String code = input.getText();

        Optional<User> userOptional = verificationService.verifyTelegramCode(code, input.getUserId());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            context.setDomainUserId(user.getId());
            context.setUserName(user.getFirstName());

            // Create verified identity
            userIdentityService.createOrUpdateTelegramIdentity(user, input.getUserId());

            log.info("Successfully verified user {} and created Telegram identity.", user.getUsername());

            context.setCurrentState(VerificationState.AWAITING_PHONE_CONNECTION_DECISION);
            return HandlerOutcome.SUCCESS;
        } else {
            return HandlerOutcome.FAILURE;
        }
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.AWAITING_VERIFICATION_CODE;
    }
}
