package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.entities.ChatbotAccount;
import pt.estga.user.enums.ChatbotPlatform;
import pt.estga.user.repositories.ChatbotAccountRepository;
import pt.estga.user.repositories.UserRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotAccountService {

    private final ChatbotAccountRepository chatbotAccountRepository;
    private final UserRepository userRepository;

    public Optional<ChatbotAccount> findByProviderAndValue(ChatbotPlatform chatbotPlatform, String value) {
        return chatbotAccountRepository.findByChatbotPlatformAndValue(chatbotPlatform, value);
    }

    @Transactional
    public ChatbotAccount createAndAssociate(User user, ChatbotPlatform chatbotPlatform, String identityValue) {
        // Verify that the user doesn't have an identity with the given chatbotPlatform yet
        boolean identityExists = chatbotAccountRepository.findByUserAndChatbotPlatform(user, chatbotPlatform).isPresent();

        if (identityExists) {
            throw new IllegalStateException("User already has an identity with chatbotPlatform " + chatbotPlatform);
        }

        ChatbotAccount identity = ChatbotAccount.builder()
                .chatbotPlatform(chatbotPlatform)
                .value(identityValue)
                .user(user)
                .build();

        return chatbotAccountRepository.save(identity);
    }

    @Transactional
    public ChatbotAccount createOrUpdateChatbot(User user, String chatId) {
        // Re-fetch the user to ensure it's attached to the current session
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + user.getId()));

        // Default platform for existing code paths was TELEGRAM. Use TELEGRAM if repository lookup expects a platform.
        Optional<ChatbotAccount> existingIdentity = chatbotAccountRepository.findByUserAndChatbotPlatform(managedUser, ChatbotPlatform.TELEGRAM);

        if (existingIdentity.isPresent()) {
            log.info("Updating chatbot account for user: {} (platform=TELEGRAM)", managedUser.getId());
            existingIdentity.get().setValue(chatId);
            return chatbotAccountRepository.save(existingIdentity.get());
        } else {
            log.info("Creating chatbot account for user: {} (platform=TELEGRAM)", managedUser.getId());
            return createAndAssociate(managedUser, ChatbotPlatform.TELEGRAM, chatId);
        }
    }

    public void delete(ChatbotAccount chatbotAccount) {
        chatbotAccountRepository.delete(chatbotAccount);
    }
}
