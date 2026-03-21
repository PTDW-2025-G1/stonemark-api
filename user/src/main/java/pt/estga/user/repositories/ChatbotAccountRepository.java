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

    Optional<ChatbotAccount> findByUserAndChatbotPlatform(User user, ChatbotPlatform chatbotPlatform);

    void deleteByUser(User user);
}
