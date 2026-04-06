package pt.estga.processing.services.draft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DraftMarkEvidenceQueryServiceTest {

    private DraftMarkEvidenceRepository repository;
    private Clock clock;
    private DraftMarkEvidenceQueryService service;

    @BeforeEach
    public void setUp() throws Exception {
        repository = mock(DraftMarkEvidenceRepository.class);
        clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);

        service = new DraftMarkEvidenceQueryService(repository, clock);

        // set staleTimeoutMinutes field via reflection to a small value for faster tests
        Field f = DraftMarkEvidenceQueryService.class.getDeclaredField("staleTimeoutMinutes");
        f.setAccessible(true);
        f.setLong(service, 1L);
    }

    private DraftMarkEvidence buildDraft(long id, ProcessingStatus status, Instant lastModified) {
        DraftMarkEvidence d = DraftMarkEvidence.builder()
                .id(id)
                .active(true)
                .processingStatus(status)
                .build();
        if (lastModified != null) {
            try {
                Class<?> c = d.getClass();
                Field field = null;
                while (c != null) {
                    try {
                        field = c.getDeclaredField("lastModifiedAt");
                        break;
                    } catch (NoSuchFieldException e) {
                        c = c.getSuperclass();
                    }
                }
                if (field != null) {
                    field.setAccessible(true);
                    field.set(d, lastModified);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return d;
    }

    @Test
    public void findSubmissionsReadyForProcessing_returnsQueuedAndStaleInProgressOnly() {
        when(repository.findDraftIdsReadyForProcessing(any())).thenReturn(List.of(1L, 2L, 3L));

        DraftMarkEvidence d1 = buildDraft(1L, ProcessingStatus.QUEUED, Instant.parse("2020-01-01T00:00:00Z"));
        when(repository.findById(1L)).thenReturn(Optional.of(d1));

        DraftMarkEvidence d2 = buildDraft(2L, ProcessingStatus.IN_PROGRESS, Instant.parse("2020-01-01T00:00:00Z").plus(Duration.ofMinutes(5)));
        when(repository.findById(2L)).thenReturn(Optional.of(d2));

        DraftMarkEvidence d3 = buildDraft(3L, ProcessingStatus.IN_PROGRESS, Instant.parse("2019-12-31T23:40:00Z"));
        when(repository.findById(3L)).thenReturn(Optional.of(d3));

        var result = service.findSubmissionsReadyForProcessing(PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.contains(1L));
        assertTrue(result.contains(3L));
        assertFalse(result.contains(2L));
    }
}
