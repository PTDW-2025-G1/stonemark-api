package pt.estga.decision.listeners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.entities.Monument;
import pt.estga.content.services.MarkOccurrenceService;
import pt.estga.content.services.MonumentService;
import pt.estga.decision.services.DecisionServiceFactory;
import pt.estga.file.entities.MediaFile;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.events.SubmissionAcceptedEvent;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionEventListenerTest {

    @Mock
    private DecisionServiceFactory decisionServiceFactory;

    @Mock
    private MarkOccurrenceSubmissionRepository proposalRepo;

    @Mock
    private MonumentService monumentService;

    @Mock
    private MarkOccurrenceService markOccurrenceService;

    private ProposalEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProposalEventListener(decisionServiceFactory, proposalRepo, monumentService, markOccurrenceService);
    }

    @Test
    void handleProposalAccepted_ShouldCreateOccurrenceAndActivateMonument() throws IOException {
        Monument monument = Monument.builder().id(10L).active(false).build();
        Mark mark = Mark.builder().id(20L).build();
        MediaFile mediaFile = MediaFile.builder().id(40L).build();
        User submittedBy = User.builder().id(30L).build();

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .existingMonument(monument)
                .existingMark(mark)
                .originalMediaFile(mediaFile)
                .submittedBy(submittedBy)
                .embedding(new float[]{1.0f, 2.0f})
                .build();

        when(proposalRepo.findById(1L)).thenReturn(Optional.of(proposal));
        when(markOccurrenceService.create(any(MarkOccurrence.class), isNull(), eq(40L)))
                .thenReturn(MarkOccurrence.builder().id(99L).build());

        listener.handleProposalAccepted(new SubmissionAcceptedEvent(this, proposal));

        verify(monumentService).update(monument);
        verify(markOccurrenceService).create(
                argThat(occurrence -> occurrence.getMark() == mark
                        && occurrence.getMonument() == monument
                        && occurrence.getAuthor() == submittedBy
                        && occurrence.getEmbedding() != null
                        && occurrence.getEmbedding().length == 2),
                isNull(),
                eq(40L)
        );
    }

    @Test
    void handleProposalAccepted_ShouldDoNothingWhenProposalNotFound() throws IOException {
        MarkOccurrenceSubmission eventProposal = MarkOccurrenceSubmission.builder().id(1L).build();
        when(proposalRepo.findById(1L)).thenReturn(Optional.empty());

        listener.handleProposalAccepted(new SubmissionAcceptedEvent(this, eventProposal));

        verify(monumentService, never()).update(any());
        verify(markOccurrenceService, never()).create(any(), any(), any());
    }

    @Test
    void handleProposalAccepted_ShouldDoNothingWhenEventProposalIsNull() throws IOException {
        listener.handleProposalAccepted(new SubmissionAcceptedEvent(this, null));

        verify(proposalRepo, never()).findById(any());
        verify(monumentService, never()).update(any());
        verify(markOccurrenceService, never()).create(any(), any(), any());
    }
}
