package pt.estga.chatbot.features.verification.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.services.ChatbotAccountFacade;

/**
 * Handles contact submission in the verification flow by linking the messaging identity
 * (Telegram/WhatsApp) to an existing domain user when the domain user id is present in context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkChatbotIdentityHandler implements ConversationStateHandler {

    private final ChatbotAccountFacade accountFacade;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.CONTACT || input.getText() == null) {
            return HandlerOutcome.FAILURE;
        }

        Long domainUserId = context.getDomainUserId();

        boolean linked = accountFacade.linkPlatformIdentity(domainUserId, "TELEGRAM", input.getUserId());
        if (linked) {
            context.setCurrentState(CoreState.MAIN_MENU);
            return HandlerOutcome.SUCCESS;
        } else {
            log.warn("Failed to link platform identity for domain user id {}", domainUserId);
            return HandlerOutcome.FAILURE;
        }
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.AWAITING_CONTACT;
    }
}
