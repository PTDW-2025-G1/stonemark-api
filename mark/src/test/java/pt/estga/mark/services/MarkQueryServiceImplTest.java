package pt.estga.mark.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.core.utils.GeometryUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkQueryServiceImplTest {

    private static final UUID MARK_ID = UUID.randomUUID();
    private static final UUID NEARBY_ID = UUID.randomUUID();

    @Mock
    private MarkRepository markRepository;
    @Mock
    private MarkOccurrenceRepository occurrenceRepository;
    @Mock
    private MarkEvidenceRepository evidenceRepository;

    private MarkQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MarkQueryServiceImpl(markRepository, occurrenceRepository, evidenceRepository);
        ReflectionTestUtils.setField(service, "occurrenceDedupeRadiusMeters", 15.0);
    }

    @Test
    @DisplayName("reuses an existing occurrence of the same mark at nearly the same place")
    void reusesNearbyOccurrence() {
        MarkOccurrence nearby = MarkOccurrence.builder()
                .id(NEARBY_ID)
                .location(GeometryUtils.createPoint(41.150000, -8.610000))
                .build();
        when(occurrenceRepository.findByMarkIdAndLocationIsNotNull(MARK_ID)).thenReturn(List.of(nearby));

        MarkOccurrenceDto result = service.findOrCreateOccurrence(MARK_ID, null, 41.150010, -8.610010);

        assertThat(result.id()).isEqualTo(NEARBY_ID);
        verify(occurrenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("creates a new occurrence when the closest one is too far")
    void createsWhenClosestIsTooFar() {
        MarkOccurrence far = MarkOccurrence.builder()
                .id(UUID.randomUUID())
                .location(GeometryUtils.createPoint(41.200000, -8.600000))
                .build();
        when(occurrenceRepository.findByMarkIdAndLocationIsNotNull(MARK_ID)).thenReturn(List.of(far));
        when(markRepository.findById(MARK_ID)).thenReturn(Optional.of(new Mark()));
        when(occurrenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MarkOccurrenceDto result = service.findOrCreateOccurrence(MARK_ID, null, 41.15, -8.61);

        assertThat(result.latitude()).isEqualTo(41.15);
        assertThat(result.longitude()).isEqualTo(-8.61);
        verify(occurrenceRepository).save(any());
    }

    @Test
    @DisplayName("creates an unlocated occurrence when no coordinates are given")
    void createsUnlocatedWhenNoCoordinates() {
        when(markRepository.findById(MARK_ID)).thenReturn(Optional.of(new Mark()));
        when(occurrenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MarkOccurrenceDto result = service.findOrCreateOccurrence(MARK_ID, null, null, null);

        assertThat(result.latitude()).isNull();
        assertThat(result.longitude()).isNull();
        verify(occurrenceRepository).save(any());
    }
}
