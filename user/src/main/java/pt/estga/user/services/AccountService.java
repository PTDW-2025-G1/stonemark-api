package pt.estga.user.services;

import pt.estga.user.dtos.AccountSecurityStatusDto;
import pt.estga.user.entities.User;

public interface AccountService {

    AccountSecurityStatusDto getSecurityStatus(User user);

}
