package pt.estga.verification.services;

import pt.estga.verification.services.commands.VerificationCommand;

public interface VerificationInitiationService {

    void initiate(VerificationCommand<Void> command);

}
