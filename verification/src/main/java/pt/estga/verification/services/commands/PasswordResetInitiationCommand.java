package pt.estga.verification.services.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.shared.exceptions.ContactMethodNotAvailableException;
import pt.estga.shared.exceptions.UserNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.ActionCodeService;
import pt.estga.verification.services.VerificationDispatchService;

@Component
@RequiredArgsConstructor
public class PasswordResetInitiationCommand implements VerificationCommand<String> {

    private final UserService userService;
    private final ActionCodeService actionCodeService;
    private final VerificationDispatchService verificationDispatchService;

    @Override
    public Runnable execute(String contactValue) {
        User user = userService.findByEmail(contactValue)
                .or(() -> userService.findByPhone(contactValue))
                .orElseThrow(() -> new UserNotFoundException("User not found with contact: " + contactValue));

        if (!user.isEnabled()) {
            throw new UserNotFoundException("User not found with contact: " + contactValue);
        }

        boolean isEmailMatch = contactValue.equals(user.getEmail());
        boolean isPhoneMatch = contactValue.equals(user.getPhone());
        boolean isVerified = (isEmailMatch && user.isEmailVerified()) || (isPhoneMatch && user.isPhoneVerified());

        if (!isVerified) {
            throw new ContactMethodNotAvailableException("Contact is not verified: " + contactValue);
        }

        ActionCode actionCode = actionCodeService.createAndSave(user, null, ActionCodeType.RESET_PASSWORD);

        return () -> verificationDispatchService.sendVerification(contactValue, actionCode);
    }
}
