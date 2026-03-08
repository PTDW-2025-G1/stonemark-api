package pt.estga.verification.services;

import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Optional;

public interface ActionCodeService {

    ActionCode createAndSave(User user, String recipient, ActionCodeType type);

    Optional<ActionCode> findByCode(String code);

    boolean isCodeValid(String code);

    void consumeCode(ActionCode code);

}
