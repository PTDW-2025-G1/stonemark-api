package pt.estga.verification.services;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface VerificationProcessingService {

    @Transactional
    Optional<String> confirmCode(String code);


}
