package pt.estga.verification.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.user.entities.User;

import java.util.Optional;

@Repository
public interface ActionCodeRepository extends JpaRepository<ActionCode, String> {

    Optional<ActionCode> findByCode(String code);

    Optional<ActionCode> findByUserAndType(User user, ActionCodeType type);

    void deleteByUserAndType(User user, ActionCodeType type);

    void deleteByTelegramIdAndType(String telegramId, ActionCodeType type);

}
