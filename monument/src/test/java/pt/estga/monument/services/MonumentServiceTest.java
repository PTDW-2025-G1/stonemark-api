package pt.estga.monument.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonumentServiceTest {

    @Mock
    private MonumentRepository repository;

    @InjectMocks
    private MonumentService service;

    @Test
    @DisplayName("should return monument when found by id")
    void shouldFindById() {
        var monument = new Monument();
        monument.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(monument));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should return empty when monument not found by id")
    void shouldReturnEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return paginated list of monuments")
    void shouldFindAll() {
        var monument = new Monument();
        monument.setId(1L);
        var page = new PageImpl<>(List.of(monument), PageRequest.of(0, 20), 1);
        when(repository.findAll(PageRequest.of(0, 20))).thenReturn(page);

        var result = service.findAll(PageRequest.of(0, 20));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should save and return monument")
    void shouldSave() {
        var monument = new Monument();
        monument.setName("New Monument");
        when(repository.save(monument)).thenReturn(monument);

        var result = service.save(monument);

        assertThat(result.getName()).isEqualTo("New Monument");
        verify(repository).save(monument);
    }

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
