package pt.estga.chatbot.features.verification.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.estga.shared.events.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.user.entities.User;
import pt.estga.user.services.ChatbotAccountService;
import pt.estga.user.services.UserService;
import pt.estga.verification.events.ChatbotAccountConnectedEvent;

import java.util.Optional;

/**
 * Handles contact submission in the verification flow by linking the messaging identity
 * (Telegram/WhatsApp) to an existing domain user when the domain user id is present in context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkChatbotIdentityHandler implements ConversationStateHandler {

    private final ChatbotAccountService chatbotAccountService;
    private final UserService userService;
    private final AfterCommitEventPublisher eventPublisher;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.CONTACT || input.getText() == null) {
            return HandlerOutcome.FAILURE;
        }

        Long domainUserId = context.getDomainUserId();

        if (domainUserId != null) {
            Optional<User> userOptional = userService.findById(domainUserId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // Associate messaging identity with domain user (Telegram for now)
                chatbotAccountService.createOrUpdateChatbot(user, input.getUserId());

                // Publish event so listeners (e.g., notification services) can notify the user
                try {
                    eventPublisher.publish(new ChatbotAccountConnectedEvent(this, "TELEGRAM", input.getUserId(), user.getId()));
                } catch (Exception e) {
                    log.error("Failed to publish ChatbotAccountConnectedEvent for user {}: {}", user.getId(), e.getMessage());
                }

                log.info("Successfully associated chatbot identity for user {}", user.getUsername());
                context.setCurrentState(CoreState.MAIN_MENU);
                return HandlerOutcome.SUCCESS;
            } else {
                log.error("User with ID {} not found in domain", domainUserId);
                return HandlerOutcome.FAILURE;
            }
        } else {
            // Without a domain user id, we cannot link the messaging identity anymore
            log.warn("No domain user id present; cannot link chatbot identity without prior login.");
            return HandlerOutcome.FAILURE;
        }
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.AWAITING_CONTACT;
    }
}
