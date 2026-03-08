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
import pt.estga.user.services.UserService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitContactHandler implements ConversationStateHandler {

    private final UserIdentityService userIdentityService;
    private final UserService userService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.CONTACT || input.getText() == null) {
            return HandlerOutcome.FAILURE;
        }

        String phoneNumber = input.getText();
        Long domainUserId = context.getDomainUserId();

        if (domainUserId != null) {
            Optional<User> userOptional = userService.findById(domainUserId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPhone(phoneNumber);
                user.setPhoneVerified(true);
                userService.update(user);
                log.info("Successfully connected phone number for user {}", user.getUsername());
                context.setCurrentState(VerificationState.PHONE_CONNECTION_SUCCESS);
                return HandlerOutcome.SUCCESS;
            } else {
                log.error("User with ID {} not found in domain", domainUserId);
                return HandlerOutcome.FAILURE;
            }
        } else {
            Optional<User> userOptional = userService.findByPhone(phoneNumber);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPhone(phoneNumber);
                user.setPhoneVerified(true);
                userService.update(user);

                userIdentityService.createOrUpdateTelegramIdentity(user, input.getUserId());

                log.info("Successfully verified user {} and associated their Telegram ID.", user.getUsername());

                context.setDomainUserId(user.getId());
                context.setUserName(user.getFirstName());
                context.setCurrentState(VerificationState.PHONE_VERIFICATION_SUCCESS);

                return HandlerOutcome.SUCCESS;
            } else {
                log.warn("No user found for phone number: {}", phoneNumber);
                return HandlerOutcome.FAILURE;
            }
        }
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.AWAITING_CONTACT;
    }
}
