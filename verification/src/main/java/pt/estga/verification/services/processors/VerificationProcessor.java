package pt.estga.verification.services.processors;

import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Optional;

/**
 * Interface for processing action codes based on their type.
 * This is part of a Strategy pattern to handle different verification outcomes.
 */
public interface VerificationProcessor {

    /**
     * Processes the given action code.
     *
     * @param recipient Optional recipient (email/phone) for dispatch-oriented flows.
     * @param code The {@link ActionCode} to be processed.
     */
    Optional<String> process(String recipient, ActionCode code);

    ActionCodeType getType();
}
