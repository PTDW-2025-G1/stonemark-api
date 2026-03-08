package pt.estga.verification.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pt.estga.user.events.EmailVerificationRequestedEvent;
import pt.estga.user.events.TelephoneVerificationRequestedEvent;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.ActionCodeService;
import pt.estga.verification.services.VerificationDispatchService;
import pt.estga.verification.services.VerificationInitiationService;
import pt.estga.verification.services.commands.ActionCodeCommand;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final VerificationInitiationService verificationInitiationService;
    private final ActionCodeService actionCodeService;
    private final VerificationDispatchService verificationDispatchService;

    @Async
    @EventListener
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        String recipient = event.getEmail();
        log.info("Handling email verification event for recipient {}", recipient);
        var command = new ActionCodeCommand(event.getUser(), recipient, actionCodeService, verificationDispatchService, ActionCodeType.EMAIL_VERIFICATION);
        verificationInitiationService.initiate(command);
    }

    @Async
    @EventListener
    public void handleTelephoneVerificationRequested(TelephoneVerificationRequestedEvent event) {
        String recipient = event.getPhone();
        log.info("Handling telephone verification event for recipient {}", recipient);
        var command = new ActionCodeCommand(event.getUser(), recipient, actionCodeService, verificationDispatchService, ActionCodeType.PHONE_VERIFICATION);
        verificationInitiationService.initiate(command);
    }
}
