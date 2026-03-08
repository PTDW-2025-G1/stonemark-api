package pt.estga.verification.services;

import pt.estga.verification.entities.ActionCode;

public interface VerificationDispatchService {

    void sendVerification(String recipient, ActionCode actionCode);

}
