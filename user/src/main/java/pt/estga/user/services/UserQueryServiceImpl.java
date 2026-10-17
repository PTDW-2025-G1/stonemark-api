package pt.estga.user.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.api.UserQueryService;
import pt.estga.user.repositories.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }
}
