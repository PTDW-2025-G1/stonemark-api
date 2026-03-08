package pt.estga.verification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import pt.estga.verification.services.commands.VerificationCommand;

/**
 * Service responsible for initiating various verification flows.
 * It acts as an invoker for different {@link VerificationCommand}s,
 * delegating the specific initiation logic to the commands themselves.
 */
@Service
@RequiredArgsConstructor
public class VerificationInitiationServiceImpl implements VerificationInitiationService {

    private final TaskExecutor taskExecutor;

    /**
     * Initiates a verification process by executing the given command.
     * This method serves as a generic entry point for all verification initiation types.
     *
     * @param command The {@link VerificationCommand} to be executed.
     */
    @Override
    public void initiate(VerificationCommand<Void> command) {
        Runnable task = command.execute(null);
        taskExecutor.execute(task);
    }
}
