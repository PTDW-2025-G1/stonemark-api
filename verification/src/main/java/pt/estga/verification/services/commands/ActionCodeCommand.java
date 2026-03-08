package pt.estga.verification.services.commands;

import lombok.RequiredArgsConstructor;
import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.ActionCodeService;
import pt.estga.verification.services.VerificationDispatchService;

@RequiredArgsConstructor
public class ActionCodeCommand implements VerificationCommand<Void> {

    private final User user;
    private final String recipient;
    private final ActionCodeService actionCodeService;
    private final VerificationDispatchService verificationDispatchService;
    private final ActionCodeType actionCodeType;

    @Override
    public Runnable execute(Void parameter) {
        ActionCode actionCode = actionCodeService.createAndSave(user, recipient, actionCodeType);
        return () -> verificationDispatchService.sendVerification(recipient, actionCode);
    }
}
