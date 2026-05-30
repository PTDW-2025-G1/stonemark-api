package pt.estga.userapi;

import pt.estga.user.dtos.ChatbotAccountDto;

public interface ChatbotAccountOperations {

    ChatbotAccountDto createOrUpdateChatbot(Long userId, String chatId);
}
