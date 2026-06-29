package pt.estga.monument.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.repositories.MonumentRepository;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonumentServiceTest {

    @Mock
    private MonumentRepository repository;

    @InjectMocks
    private MonumentService service;

    @Test
    @DisplayName("should soft-delete monument when found")
    void shouldDeleteById() {
        var monument = new Monument();
        monument.setId(1L);
        monument.activate();
        when(repository.findById(1L)).thenReturn(Optional.of(monument));

        service.deleteById(1L);

        verify(repository).softDelete(monument);
    }

    @Test
    @DisplayName("should throw when deleting non-existent monument")
    void shouldThrowWhenDeletingNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
