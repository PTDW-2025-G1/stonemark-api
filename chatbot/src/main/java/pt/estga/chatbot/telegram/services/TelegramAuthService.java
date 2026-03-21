package pt.estga.chatbot.telegram.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.AuthService;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.enums.ChatbotPlatform;
import pt.estga.user.services.ChatbotAccountService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService implements AuthService {

    private final ChatbotAccountService chatbotAccountService;

    @Override
    public boolean isAuthenticated(String platformUserId) {
        return chatbotAccountService.findByProviderAndValue(ChatbotPlatform.TELEGRAM, platformUserId).isPresent();
    }

    @Override
    public Optional<AppPrincipal> authenticate(String platformUserId) {
        return chatbotAccountService.findByProviderAndValue(ChatbotPlatform.TELEGRAM, platformUserId)
                .map(userIdentity -> {
                    var user = userIdentity.getUser();
                    return AppPrincipal.builder()
                            .id(user.getId())
                            .identifier(user.getUsername())
                            .password(null)
                            .authorities(SecurityUtils.mapUserRolesToAuthorities(user.getRole()))
                            .enabled(user.isEnabled())
                            .accountNonLocked(!user.isAccountLocked())
                            .build();
                });
    }

    @Override
    public boolean supports(Platform platform) {
        return Platform.TELEGRAM.equals(platform);
    }
}
