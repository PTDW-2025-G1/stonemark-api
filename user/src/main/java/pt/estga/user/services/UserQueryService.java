package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.estga.shared.filters.models.FilterNode;
import pt.estga.shared.filters.SpecificationBuilder;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

/**
 * Service for querying User entities based on dynamic filters.
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final SpecificationBuilder<User> specificationBuilder;

    /**
     * Searches for users based on the provided filter and pageable information.
     *
     * @param filter   the filter criteria represented as a FilterNode
     * @param pageable the pagination information
     * @return a paginated list of users matching the filter criteria
     */
    public Page<User> search(FilterNode filter, Pageable pageable) {
        return userRepository.findAll(specificationBuilder.build(filter), pageable);
    }
}
