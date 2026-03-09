package pt.estga.user.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.user.entities.User;
import pt.estga.user.entities.ChatbotAccount;
import pt.estga.user.enums.ChatbotPlatform;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotAccountRepository extends JpaRepository<ChatbotAccount, Long> {

    Optional<ChatbotAccount> findByChatbotPlatformAndValue(ChatbotPlatform chatbotPlatform, String value);

    void deleteByUserAndChatbotPlatform(User user, ChatbotPlatform chatbotPlatform);

    Optional<ChatbotAccount> findByUserAndChatbotPlatform(User user, ChatbotPlatform chatbotPlatform);

    List<ChatbotAccount> findByUser(User user);

    void deleteByUser(User user);
}
