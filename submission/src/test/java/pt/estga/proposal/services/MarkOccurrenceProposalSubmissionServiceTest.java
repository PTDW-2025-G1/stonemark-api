package pt.estga.proposal.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import pt.estga.proposal.dtos.MarkOccurrenceProposalCreateDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.enums.SubmissionSource;
import pt.estga.proposal.events.ProposalSubmittedEvent;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;
import pt.estga.proposal.services.submission.MarkOccurrenceProposalSubmissionService;
import pt.estga.user.entities.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarkOccurrenceProposalSubmissionServiceTest {

    @Mock
    private MarkOccurrenceProposalRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MarkOccurrenceProposalSubmissionService submissionService;

    @Test
    void createAndSubmit_ShouldCreateAndSubmitProposal() {
        // Arrange
        User user = User.builder().id(1L).build();
        MarkOccurrenceProposalCreateDto dto = new MarkOccurrenceProposalCreateDto(
                40.64, -8.65, "Found this mark", SubmissionSource.WEB_APP, null, null, 100L
        );

        MarkOccurrenceProposal savedProposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .status(ProposalStatus.SUBMITTED)
                .build();

        when(repository.save(any(MarkOccurrenceProposal.class))).thenReturn(savedProposal);

        // Act
        MarkOccurrenceProposal result = submissionService.createAndSubmit(dto, user);

        // Assert
        assertNotNull(result);
        assertEquals(ProposalStatus.SUBMITTED, result.getStatus());
        
        verify(repository).save(any(MarkOccurrenceProposal.class));
        verify(eventPublisher).publishEvent(any(ProposalSubmittedEvent.class));
    }

    @Test
    void submit_ShouldNotResubmit_IfAlreadySubmitted() {
        // Arrange
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .status(ProposalStatus.SUBMITTED)
                .build();

        // Act
        MarkOccurrenceProposal result = submissionService.submit(proposal);

        // Assert
        assertEquals(ProposalStatus.SUBMITTED, result.getStatus());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
