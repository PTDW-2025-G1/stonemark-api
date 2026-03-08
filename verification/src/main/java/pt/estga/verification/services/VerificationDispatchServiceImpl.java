package pt.estga.verification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.user.entities.UserContact;
import pt.estga.verification.entities.ActionCode;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationDispatchServiceImpl implements VerificationDispatchService {

    private final ActionCodeDispatchService actionCodeDispatchService;

    @Override
    public void sendVerification(UserContact userContact, ActionCode actionCode) {
        String recipient = userContact != null ? userContact.getValue() : actionCode.getUser() != null ? actionCode.getUser().getEmail() : "unknown";
        log.info("Dispatching verification for recipient {} with action code id {}", recipient, actionCode.getId());
        try {
            actionCodeDispatchService.sendVerification(userContact, actionCode);
            log.info("Successfully dispatched verification for recipient {}", recipient);
        } catch (Exception e) {
            log.error("Error dispatching verification for recipient {}", recipient, e);
            throw e;
        }
    }

    @Override
    public void sendVerification(String recipient, ActionCode actionCode) {
        log.info("Dispatching verification for recipient {} with action code id {}", recipient, actionCode.getId());
        try {
            actionCodeDispatchService.sendVerification(recipient, actionCode);
            log.info("Successfully dispatched verification for recipient {}", recipient);
        } catch (Exception e) {
            log.error("Error dispatching verification for recipient {}", recipient, e);
            throw e;
        }
    }
}
