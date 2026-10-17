package pt.estga.intake.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.shared.core.interfaces.DivisionLookupClient;
import pt.estga.file.api.FileStorageOperations;
import pt.estga.file.api.StagedFileRecord;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionFacadeTest {

    private static final Long EXISTING_ID = 7L;
    private static final Long CREATED_ID = 42L;
    private static final UUID STAGED_ID = UUID.randomUUID();

    @Mock
    private FileStorageOperations fileStorage;

    @Mock
    private MarkEvidenceSubmissionSubmitService submitService;

    @Mock
    private MarkEvidenceSubmissionRepository submissionRepository;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private DivisionLookupClient divisionLookupClient;

    @InjectMocks
    private SubmissionFacade facade;

    @Test
    @DisplayName("rejects an empty photo before doing any work")
    void rejectsEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> facade.submitFromWeb(file, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(fileStorage, submitService);
    }

    @Test
    @DisplayName("replaying an idempotency key returns the stored submission without re-staging")
    void replaysIdempotencyKey() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(Boolean.FALSE);

        MarkEvidenceSubmission existing = MarkEvidenceSubmission.builder().id(EXISTING_ID).build();
        SubmissionDto expected = dto(EXISTING_ID);
        when(submissionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));
        when(submissionMapper.toDto(existing)).thenReturn(expected);

        SubmissionDto result = facade.submitFromWeb(
                file, 1.0, 2.0, "note", "key-1");

        assertThat(result).isSameAs(expected);
        verifyNoInteractions(fileStorage, submitService);
    }

    @Test
    @DisplayName("stages, persists and returns the created submission")
    void createsSubmission() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(Boolean.FALSE);
        when(file.getOriginalFilename()).thenReturn("mark.jpg");
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(fileStorage.stage(any(), eq("mark.jpg")))
                .thenReturn(new StagedFileRecord(STAGED_ID, "mark.jpg", 10L));
        when(divisionLookupClient.resolveByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(submitService.submit(any(MarkEvidenceSubmission.class), eq(STAGED_ID), eq("mark.jpg")))
                .thenReturn(CREATED_ID);

        MarkEvidenceSubmission saved = MarkEvidenceSubmission.builder().id(CREATED_ID).build();
        SubmissionDto expected = dto(CREATED_ID);
        when(submissionRepository.findById(CREATED_ID)).thenReturn(Optional.of(saved));
        when(submissionMapper.toDto(saved)).thenReturn(expected);

        SubmissionDto result = facade.submitFromWeb(
                file, 1.0, 2.0, "note", null);

        assertThat(result).isSameAs(expected);
        verify(submitService).submit(any(MarkEvidenceSubmission.class), eq(STAGED_ID), eq("mark.jpg"));
    }

    private static SubmissionDto dto(Long id) {
        return new SubmissionDto(id, SubmissionStatus.RECEIVED, SubmissionSource.WEB_APP,
                Instant.now(), null, null, null, null, null, null, null);
    }
}
