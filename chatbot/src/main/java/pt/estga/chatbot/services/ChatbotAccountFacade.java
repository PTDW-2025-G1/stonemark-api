package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.events.ChatbotAccountConnectedEventPublisher;
import pt.estga.user.entities.User;
import pt.estga.user.services.ChatbotAccountService;
import pt.estga.user.services.UserService;

/**
 * Facade encapsulating the logic to link a messaging platform identity to a domain user
 * and publish the resulting event. This keeps handlers thin and focused on input/flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotAccountFacade {

    private final ChatbotAccountService chatbotAccountService;
    private final UserService userService;
    private final ChatbotAccountConnectedEventPublisher eventPublisher;

    public boolean linkPlatformIdentity(Long domainUserId, String platform, String platformUserId) {
        if (domainUserId == null) return false;

        return userService.findById(domainUserId).map(user -> {
            try {
                chatbotAccountService.createOrUpdateChatbot(user, platformUserId);
                eventPublisher.publish(platform, platformUserId, user.getId());
                log.info("Linked platform identity {} for user {}", platformUserId, user.getId());
                return true;
            } catch (Exception e) {
                log.error("Failed to link platform identity for user {}: {}", user.getId(), e.getMessage());
                return false;
            }
        }).orElse(false);
    }
}
