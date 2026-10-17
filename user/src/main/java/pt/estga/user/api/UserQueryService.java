package pt.estga.user.api;

import java.util.UUID;

public interface UserQueryService {

    boolean existsById(UUID userId);
}
