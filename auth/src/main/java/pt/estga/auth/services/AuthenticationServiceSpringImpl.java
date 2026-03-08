package pt.estga.auth.services;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.auth.dtos.AuthenticationResponseDto;
import pt.estga.auth.services.tfa.TotpService;
import pt.estga.auth.services.tfa.TwoFactorAuthenticationService;
import pt.estga.security.enums.TokenType;
import pt.estga.security.services.AccessTokenService;
import pt.estga.security.services.JwtService;
import pt.estga.security.services.RefreshTokenService;
import pt.estga.shared.enums.PrincipalType;
import pt.estga.shared.enums.UserRole;
import pt.estga.shared.exceptions.EmailVerificationRequiredException;
import pt.estga.shared.exceptions.InvalidCredentialsException;
import pt.estga.shared.exceptions.UsernameAlreadyTakenException;
import pt.estga.user.entities.User;
import pt.estga.user.enums.TfaMethod;
import pt.estga.user.services.UserService;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = {EmailVerificationRequiredException.class})
public class AuthenticationServiceSpringImpl implements AuthenticationService {

    private final UserService userService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final TwoFactorAuthenticationService twoFactorAuthenticationService;

    @Value("${application.security.contact.verification-required:false}")
    private boolean contactVerificationRequired;

    @Override
    public Optional<AuthenticationResponseDto> register(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        if (userService.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyTakenException("Username already in use");
        }

        if (user.getEmail() != null && userService.existsByEmail(user.getEmail())) {
            throw new UsernameAlreadyTakenException("Email already in use");
        }

        if (user.getPhone() != null && userService.existsByPhone(user.getPhone())) {
            throw new UsernameAlreadyTakenException("Phone already in use");
        }

        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        user.setTfaMethod(TfaMethod.NONE);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User createdUser = userService.create(user);

        return generateAuthenticationResponse(createdUser, false, false);
    }

    @NotNull
    private Optional<AuthenticationResponseDto> generateAuthenticationResponse(User user, boolean tfaRequired, boolean tfaCodeSent) {
        return getAuthenticationResponseDto(user, tfaRequired, tfaCodeSent, jwtService, refreshTokenService, accessTokenService);
    }

    @NotNull
    public static Optional<AuthenticationResponseDto> getAuthenticationResponseDto(User user, boolean tfaRequired, boolean tfaCodeSent, JwtService jwtService, RefreshTokenService refreshTokenService, AccessTokenService accessTokenService) {
        if (tfaRequired) {
            return Optional.of(new AuthenticationResponseDto(null, null, user.getRole().name(), user.getTfaMethod() != TfaMethod.NONE, true, tfaCodeSent));
        }

        var tokens = jwtService.generateTokens(PrincipalType.USER, user.getId(), user.getUsername(), Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
        var accessTokenString = tokens.get(TokenType.ACCESS);
        var refreshTokenString = tokens.get(TokenType.REFRESH);

        var refreshToken = refreshTokenService.createToken(user.getId(), refreshTokenString);
        accessTokenService.createToken(user.getId(), accessTokenString, refreshToken);

        return Optional.of(new AuthenticationResponseDto(accessTokenString, refreshTokenString, user.getRole().name(), user.getTfaMethod() != TfaMethod.NONE, false, tfaCodeSent));
    }

    @Override
    public Optional<AuthenticationResponseDto> authenticate(String usernameOrContact, String password, String tfaCode) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usernameOrContact, password));
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", usernameOrContact, e);
            throw new InvalidCredentialsException();
        }

        User user = userService.findByUsername(usernameOrContact)
                .or(() -> userService.findByEmail(usernameOrContact))
                .or(() -> userService.findByPhone(usernameOrContact))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (contactVerificationRequired && !user.isEnabled()) {
            throw new EmailVerificationRequiredException("Email verification required.");
        }

        boolean tfaCodeSent = false;
        if (user.getTfaMethod() != TfaMethod.NONE) {
            if (tfaCode == null || tfaCode.isBlank()) {
                switch (user.getTfaMethod()) {
                    case TOTP:
                        break;
                    case SMS:
                        twoFactorAuthenticationService.generateAndSendSmsCode(user);
                        tfaCodeSent = true;
                        break;
                    case EMAIL:
                        twoFactorAuthenticationService.generateAndSendEmailCode(user);
                        tfaCodeSent = true;
                        break;
                }
                return generateAuthenticationResponse(user, true, tfaCodeSent);
            }

            boolean isCodeValid = switch (user.getTfaMethod()) {
                case TOTP -> totpService.isCodeValid(user.getTfaSecret(), tfaCode);
                case SMS, EMAIL ->
                        twoFactorAuthenticationService.verifyCode(user, tfaCode, ActionCodeType.TWO_FACTOR);
                default -> false;
            };

            if (!isCodeValid) {
                log.warn("Invalid 2FA code for user: {}", usernameOrContact);
                throw new InvalidCredentialsException();
            }
        }

        return generateAuthenticationResponse(user, false, false);
    }


    @Override
    @Transactional
    public Optional<AuthenticationResponseDto> refreshToken(String refreshTokenString) {
        return refreshTokenService.findByToken(refreshTokenString)
                .filter(token -> !token.isRevoked())
                .filter(refreshToken -> jwtService.isTokenValid(refreshTokenString, TokenType.REFRESH))
                .flatMap(refreshToken -> userService.findById(refreshToken.getUserId())
                        .map(user -> {
                            accessTokenService.revokeAllByRefreshToken(refreshToken);

                            String newAccessToken = jwtService.generateAccessToken(PrincipalType.USER, user.getId(), user.getUsername(), Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
                            accessTokenService.createToken(user.getId(), newAccessToken, refreshToken);

                            return new AuthenticationResponseDto(newAccessToken, refreshTokenString, user.getRole().name(), user.getTfaMethod() != TfaMethod.NONE, false, false);
                        }));
    }

    @Override
    public void logoutFromAllDevices(User user) {
        refreshTokenService.revokeAllByUserId(user.getId());
        accessTokenService.revokeAllByUserId(user.getId());
    }
}
