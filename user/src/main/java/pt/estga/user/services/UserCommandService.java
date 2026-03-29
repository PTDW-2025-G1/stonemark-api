package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.repositories.UserRepository;
import pt.estga.user.repositories.ChatbotAccountRepository;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository repository;
    private final ChatbotAccountRepository chatbotAccountRepository;
    private final UserMapper mapper;

    public User create(User user) {
        return repository.save(user);
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new ResourceNotFoundException("User id must not be null for update");
        }

        User existing = repository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + user.getId() + " not found"));

        mapper.update(user, existing);

        return repository.save(existing);
    }

    public UserDto updateFromDto(Long id, UserDto dto) {
        if (id == null) {
            throw new IllegalArgumentException("Id must be provided");
        }

        User existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        mapper.updateFromDto(dto, existing);

        User saved = repository.save(existing);
        return mapper.toDto(saved);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void softDeleteUser(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        chatbotAccountRepository.deleteByUser(user);

        repository.softDelete(user);
    }
}
