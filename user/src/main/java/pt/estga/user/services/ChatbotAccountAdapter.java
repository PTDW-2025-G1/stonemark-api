package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.user.dtos.ChatbotAccountDto;
import pt.estga.user.repositories.UserRepository;
import pt.estga.userapi.ChatbotAccountOperations;

@Component
@RequiredArgsConstructor
public class ChatbotAccountAdapter implements ChatbotAccountOperations {

    private final ChatbotAccountService chatbotAccountService;
    private final UserRepository userRepository;

    @Override
    public ChatbotAccountDto createOrUpdateChatbot(Long userId, String chatId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        var account = chatbotAccountService.createOrUpdateChatbot(user, chatId);
        return new ChatbotAccountDto(
                account.getId(),
                account.getUser().getId(),
                account.getChatbotPlatform().name(),
                account.getValue()
        );
    }
}
