package pt.estga.monument.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.estga.monument.dtos.MonumentListDto;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.mappers.MonumentMapper;
import pt.estga.monument.services.MonumentService;
import pt.estga.shared.web.exceptions.GlobalExceptionHandler;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MonumentControllerTest {

    private final MonumentService service = mock();
    private final MonumentMapper mapper = new MonumentMapper();

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new MonumentController(service)
    ).setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    private Monument monument;

    @BeforeEach
    void setUp() {
        monument = new Monument();
        monument.setId(1L);
        monument.setName("Test Monument");
    }

    @Test
    @DisplayName("should return 200 and monument when found by id")
    void shouldReturnMonumentWhenFound() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(mapper.toResponseDto(monument)));

        mockMvc.perform(get("/api/v1/public/monuments/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Monument"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("should return 404 when monument not found by id")
    void shouldReturn404WhenNotFound() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/public/monuments/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should return paginated monument list")
    void shouldReturnPaginatedList() throws Exception {
        Page<MonumentListDto> page = new PageImpl<>(List.of(mapper.toListDto(monument)), PageRequest.of(0, 20), 1);
        when(service.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/public/monuments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Monument"));
    }
}
