package pt.estga.chatbot.whatsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.AuthService;
import pt.estga.commoncore.models.AppPrincipal;
import pt.estga.user.enums.ChatbotPlatform;
import pt.estga.user.repositories.ChatbotAccountRepository;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WhatsAppAuthService implements AuthService {

    private final ChatbotAccountRepository chatbotAccountRepository;

    @Override
    public boolean isAuthenticated(String platformUserId) {
        return chatbotAccountRepository.findByChatbotPlatformAndValue(ChatbotPlatform.WHATSAPP, platformUserId).isPresent();
    }

    @Override
    public Optional<AppPrincipal> authenticate(String platformUserId) {
        return chatbotAccountRepository.findByChatbotPlatformAndValue(ChatbotPlatform.WHATSAPP, platformUserId)
                .map(userIdentity -> {
                    var user = userIdentity.getUser();
                    return AppPrincipal.builder()
                            .id(user.getId())
                            .identifier(user.getUsername())
                            .password(null)
                            .authorities(user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                                    .collect(Collectors.toUnmodifiableSet()))
                            .enabled(user.isEnabled())
                            .accountNonLocked(!user.isAccountLocked())
                            .build();
                });
    }

    @Override
    public boolean supports(Platform platform) {
        return Platform.WHATSAPP.equals(platform);
    }
}
