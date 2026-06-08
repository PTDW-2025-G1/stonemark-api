package pt.estga.boot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String googleSub = oAuth2User.getName();
            User user = userRepository.findByGoogleSub(googleSub).orElse(null);

            if (user != null) {
                String accessToken = jwtService.generateAccessToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                String redirectUrl = getDefaultTargetUrl();
                if (redirectUrl == null || redirectUrl.isBlank()) {
                    redirectUrl = "/";
                }

                String separator = redirectUrl.contains("?") ? "&" : "?";
                String targetUrl = redirectUrl + separator
                        + "accessToken=" + accessToken
                        + "&refreshToken=" + refreshToken
                        + "&userId=" + user.getId();

                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google authentication failed");
    }
}
