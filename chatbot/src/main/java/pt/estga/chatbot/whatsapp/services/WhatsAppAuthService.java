package pt.estga.chatbot.whatsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.AuthService;
import pt.estga.shared.enums.PrincipalType;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.enums.Provider;
import pt.estga.user.services.UserIdentityService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WhatsAppAuthService implements AuthService {

    private final UserIdentityService userIdentityService;

    @Override
    public boolean isAuthenticated(String platformUserId) {
        return userIdentityService.findByProviderAndValue(Provider.WHATSAPP, platformUserId).isPresent();
    }

    @Override
    public Optional<AppPrincipal> authenticate(String platformUserId) {
        return userIdentityService.findByProviderAndValue(Provider.WHATSAPP, platformUserId)
                .map(userIdentity -> {
                    var user = userIdentity.getUser();
                    return AppPrincipal.builder()
                            .id(user.getId())
                            .type(PrincipalType.USER)
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
        return Platform.WHATSAPP.equals(platform);
    }
}
