package pt.estga.user.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.dtos.LinkedProviderDto;
import pt.estga.user.entities.User;
import pt.estga.user.entities.ChatbotAccount;
import pt.estga.user.enums.ChatbotPlatform;
import pt.estga.user.repositories.ChatbotAccountRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class AccountSocialService {

    private final UserService userService;
    private final ChatbotAccountService chatbotAccountService;
    private final ChatbotAccountRepository chatbotAccountRepository;

    public List<LinkedProviderDto> getLinkedProviders(User user) {
        User managedUser = userService
                .findById(user.getId())
                .orElseThrow();

        List<ChatbotAccount> identities = chatbotAccountRepository.findByUser(managedUser);

        return identities.stream()
                .map(identity -> new LinkedProviderDto(identity.getChatbotPlatform()))
                .toList();
    }

    @Transactional
    public void unlinkSocialAccount(User user, ChatbotPlatform chatbotPlatform) {

        User managedUser = userService
                .findById(user.getId())
                .orElseThrow();

        chatbotAccountService.deleteByUserAndProvider(managedUser, chatbotPlatform);
    }
}
