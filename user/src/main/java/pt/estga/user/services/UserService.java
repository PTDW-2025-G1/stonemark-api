package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.repositories.UserRepository;
import pt.estga.user.repositories.ChatbotAccountRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final ChatbotAccountRepository chatbotAccountRepository;

    public User create(User user) {
        return repository.save(user);
    }

    public User update(User user) {
        return repository.save(user);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void softDeleteUser(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        chatbotAccountRepository.deleteByUser(user);

        repository.softDelete(user);
    }
}
