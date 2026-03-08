package pt.estga.verification.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.processors.VerificationProcessor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionCodeDispatchServiceImpl implements ActionCodeDispatchService {

    private final List<VerificationProcessor> processors;
    private Map<ActionCodeType, VerificationProcessor> processorsMap;

    @PostConstruct
    public void init() {
        log.info("Initializing ActionCodeDispatchServiceImpl with {} processors.", processors.size());
        processorsMap = processors.stream()
                .collect(Collectors.toMap(VerificationProcessor::getType, Function.identity()));
        log.info("Processors map initialized with keys: {}", processorsMap.keySet());
    }

    @Override
    public void sendVerification(String recipient, ActionCode code) {
        log.info("ActionCodeDispatchService: Sending verification for recipient {} with code type {}", recipient, code.getType());
        try {
            VerificationProcessor processor = processorsMap.get(code.getType());
            if (processor == null) {
                log.error("No verification processor found for action code type: {}", code.getType());
                return;
            }
            log.info("Found processor {} for type {}", processor.getClass().getSimpleName(), code.getType());
            processor.process(recipient, code);
            log.info("ActionCodeDispatchService: Successfully processed verification for action code {}", code.getId());
        } catch (Exception e) {
            log.error("Error during action code dispatch for action code {}", code.getId(), e);
            throw e;
        }
    }
}
