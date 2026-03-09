package pt.estga.verification.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Optional;

@Repository
public interface ActionCodeRepository extends JpaRepository<ActionCode, Long> {

    Optional<ActionCode> findByCode(String code);

    void deleteByTelegramIdAndType(String telegramId, ActionCodeType type);

}
