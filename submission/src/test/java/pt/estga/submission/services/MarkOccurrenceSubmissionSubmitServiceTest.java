package pt.estga.submission.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import pt.estga.submission.dtos.MarkOccurrenceProposalCreateDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.enums.SubmissionSource;
import pt.estga.submission.events.SubmissionSubmittedEvent;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarkOccurrenceSubmissionSubmitServiceTest {

    @Mock
    private MarkOccurrenceSubmissionRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MarkOccurrenceSubmissionSubmitService submissionService;

    @Test
    void createAndSubmit_ShouldCreateAndSubmitProposal() {
        // Arrange
        User user = User.builder().id(1L).build();
        MarkOccurrenceProposalCreateDto dto = new MarkOccurrenceProposalCreateDto(
                40.64, -8.65, "Found this mark", SubmissionSource.WEB_APP, null, null, 100L
        );

        MarkOccurrenceSubmission savedProposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        when(repository.save(any(MarkOccurrenceSubmission.class))).thenReturn(savedProposal);

        // Act
        MarkOccurrenceSubmission result = submissionService.createAndSubmit(dto, user);

        // Assert
        assertNotNull(result);
        assertEquals(SubmissionStatus.SUBMITTED, result.getStatus());
        
        verify(repository).save(any(MarkOccurrenceSubmission.class));
        verify(eventPublisher).publishEvent(any(SubmissionSubmittedEvent.class));
    }

    @Test
    void submit_ShouldNotResubmit_IfAlreadySubmitted() {
        // Arrange
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        // Act
        MarkOccurrenceSubmission result = submissionService.submit(proposal);

        // Assert
        assertEquals(SubmissionStatus.SUBMITTED, result.getStatus());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
