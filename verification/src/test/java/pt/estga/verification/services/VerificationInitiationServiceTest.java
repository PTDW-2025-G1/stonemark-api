package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import pt.estga.verification.services.commands.VerificationCommand;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationInitiationServiceTest {

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private VerificationInitiationServiceImpl verificationInitiationService;

    @Mock
    private VerificationCommand<Void> mockVerificationCommand;

    private Runnable mockRunnable;

    @BeforeEach
    void setUp() {
        mockRunnable = mock(Runnable.class);
    }

    @Test
    void initiate_shouldExecuteGenericCommandAndSubmitToTaskExecutor() {
        when(mockVerificationCommand.execute(any())).thenReturn(mockRunnable);

        verificationInitiationService.initiate(mockVerificationCommand);

        verify(mockVerificationCommand, times(1)).execute(isNull());
        verify(taskExecutor, times(1)).execute(mockRunnable);
    }
}
