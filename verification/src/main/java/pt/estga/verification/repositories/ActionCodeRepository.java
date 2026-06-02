package pt.estga.verification.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ActionCodeRepository extends JpaRepository<ActionCode, Long> {

    Optional<ActionCode> findByCode(String code);

    @Modifying
    @Query("UPDATE ActionCode a SET a.consumed = true WHERE a.code = :code AND a.consumed = false")
    int markConsumed(@Param("code") String code);

    void deleteByPlatformUserIdAndType(String telegramId, ActionCodeType type);

    @Modifying
    int deleteByExpiresAtBefore(Instant now);

}
