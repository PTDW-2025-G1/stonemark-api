package pt.estga.shared.services;

/**
 * SMS service (deprecated - phone support removal in progress).
 * Implementations may be removed in Phase 4.
 */
public interface SmsService {

    void sendMessage(String phoneNumber, String code);

}
