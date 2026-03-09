package pt.estga.user.services;

import pt.estga.user.entities.User;
import pt.estga.user.entities.ChatbotAccount;
import pt.estga.user.enums.ChatbotPlatform;

import java.util.Optional;

public interface ChatbotAccountService {

    ChatbotAccount createAndAssociate(User user, ChatbotPlatform chatbotPlatform, String identityValue);

    ChatbotAccount createOrUpdateChatbot(User user, String chatId);

    Optional<ChatbotAccount> findByProviderAndValue(ChatbotPlatform chatbotPlatform, String value);

    Optional<ChatbotAccount> findByUserAndProvider(User user, ChatbotPlatform chatbotPlatform);

    void delete(ChatbotAccount chatbotAccount);

    void deleteByUserAndProvider(User user, ChatbotPlatform chatbotPlatform);

}
