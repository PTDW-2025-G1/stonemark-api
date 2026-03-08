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

        User managedUser = userService
                .findById(user.getId())
                .orElseThrow();

        boolean hasPassword =
                managedUser.getPassword() != null &&
                !managedUser.getPassword().isBlank();

        return new AccountSecurityStatusDto(hasPassword);
    }
}
