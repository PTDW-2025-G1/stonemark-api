package pt.estga.verification.services;

import pt.estga.user.entities.UserContact;
import pt.estga.verification.entities.ActionCode;

public interface ActionCodeDispatchService {

    void sendVerification(UserContact userContact, ActionCode code);

    void sendVerification(String recipient, ActionCode code);

}
