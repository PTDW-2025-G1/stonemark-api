package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.dtos.AccountSecurityStatusDto;
import pt.estga.user.entities.User;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserService userService;

    @Override
    @Transactional
    public AccountSecurityStatusDto getSecurityStatus(User user) {

        userService.findById(user.getId()).orElseThrow();

        // Passwords are managed by Keycloak, not by local user records.
        return new AccountSecurityStatusDto(false);
    }
}
