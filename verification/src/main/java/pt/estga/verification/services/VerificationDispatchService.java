package pt.estga.verification.services;

import pt.estga.verification.entities.ActionCode;
import pt.estga.user.entities.UserContact;

public interface VerificationDispatchService {

    void sendVerification(UserContact userContact, ActionCode actionCode);

    void sendVerification(String recipient, ActionCode actionCode);

}
