package pt.estga.monument.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.mappers.MonumentMapper;
import pt.estga.monument.repositories.MonumentRepository;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonumentServiceTest {

    private static final UUID MONUMENT_ID = UUID.randomUUID();
    private static final UUID MISSING_ID = UUID.randomUUID();

    @Mock
    private MonumentRepository repository;

    @Mock
    private MonumentMapper mapper;

    @InjectMocks
    private MonumentService service;

    @Test
    @DisplayName("should soft-delete monument when found")
    void shouldDeleteById() {
        var monument = new Monument();
        monument.setId(MONUMENT_ID);
        monument.activate();
        when(repository.findById(MONUMENT_ID)).thenReturn(Optional.of(monument));

        service.deleteById(MONUMENT_ID);

        verify(repository).softDelete(monument);
    }

    @Test
    @DisplayName("should throw when deleting non-existent monument")
    void shouldThrowWhenDeletingNotFound() {
        when(repository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(MISSING_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(MISSING_ID.toString());
    }
}
