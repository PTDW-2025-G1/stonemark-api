package pt.estga.submission.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.submission.dtos.MarkOccurrenceProposalCreateDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.enums.SubmissionSource;
import pt.estga.submission.events.SubmissionSubmittedEvent;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.io.IOException;

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

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MarkOccurrenceSubmissionSubmitService submissionService;

    @Test
    void createAndSubmit_ShouldCreateAndSubmitProposal_WithImageFile() throws IOException {
        // Arrange
        User user = User.builder().id(1L).build();
        MarkOccurrenceProposalCreateDto dto = new MarkOccurrenceProposalCreateDto(
                40.64, -8.65, "Found this mark", SubmissionSource.WEB_APP, null, null, null
        );

        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        when(imageFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(imageFile.getInputStream()).thenReturn(null);

        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        when(mediaService.save(any(), any())).thenReturn(mediaFile);

        MarkOccurrenceSubmission savedProposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .status(SubmissionStatus.SUBMITTED)
                .originalMediaFile(mediaFile)
                .build();

        when(repository.save(any(MarkOccurrenceSubmission.class))).thenReturn(savedProposal);

        // Act
        MarkOccurrenceSubmission result = submissionService.createAndSubmit(dto, user, imageFile);

        // Assert
        assertNotNull(result);
        assertEquals(SubmissionStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getOriginalMediaFile());

        verify(mediaService).save(any(), any());
        verify(repository).save(any(MarkOccurrenceSubmission.class));
        verify(eventPublisher).publishEvent(any(SubmissionSubmittedEvent.class));
    }

    @Test
    void createAndSubmit_ShouldCreateAndSubmitProposal_WithoutImageFile() throws IOException {
        // Arrange
        User user = User.builder().id(1L).build();
        MarkOccurrenceProposalCreateDto dto = new MarkOccurrenceProposalCreateDto(
                40.64, -8.65, "Found this mark", SubmissionSource.WEB_APP, null, null, null
        );

        MarkOccurrenceSubmission savedProposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        when(repository.save(any(MarkOccurrenceSubmission.class))).thenReturn(savedProposal);

        // Act
        MarkOccurrenceSubmission result = submissionService.createAndSubmit(dto, user, null);

        // Assert
        assertNotNull(result);
        assertEquals(SubmissionStatus.SUBMITTED, result.getStatus());
        
        verify(mediaService, never()).save(any(), any());
        verify(repository).save(any(MarkOccurrenceSubmission.class));
        verify(eventPublisher).publishEvent(any(SubmissionSubmittedEvent.class));
    }

    @Test
    void submit_ShouldNotResubmit_IfAlreadySubmitted() throws IOException {
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
