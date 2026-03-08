package pt.estga.verification.services;

import pt.estga.verification.entities.ActionCode;

public interface ActionCodeDispatchService {

    void sendVerification(String recipient, ActionCode code);

}
