package pt.estga.chatbot.telegram.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.AuthService;
import pt.estga.commoncore.models.AppPrincipal;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService implements AuthService {

    private final UserRepository userRepository;

    @Override
    public boolean isAuthenticated(String platformUserId) {
        return userRepository.findByTelegramChatId(platformUserId).isPresent();
    }

    @Override
    public Optional<AppPrincipal> authenticate(String platformUserId) {
        return userRepository.findByTelegramChatId(platformUserId)
                .map(this::toPrincipal);
    }

    private AppPrincipal toPrincipal(User user) {
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
    }

    @Override
    public boolean supports(Platform platform) {
        return Platform.TELEGRAM.equals(platform);
    }
}
