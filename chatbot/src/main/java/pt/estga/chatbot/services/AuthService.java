package pt.estga.chatbot.services;

import pt.estga.chatbot.models.Platform;
import pt.estga.sharedcore.models.AppPrincipal;

import java.util.Optional;

public interface AuthService {

    boolean isAuthenticated(String platformUserId);

    Optional<AppPrincipal> authenticate(String platformUserId);

    boolean supports(Platform platform);

}
