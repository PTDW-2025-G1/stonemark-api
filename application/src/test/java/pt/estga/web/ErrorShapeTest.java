package pt.estga.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.shared.web.exceptions.DuplicateResourceException;
import pt.estga.shared.web.exceptions.GlobalExceptionHandler;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the single error shape ({@code status}/{@code code}/{@code message}) and
 * the HTTP status mapping, since clients depend on both.
 */
class ErrorShapeTest {

    @RestController
    static class ThrowingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("no such mark");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new DuplicateResourceException("duplicate bookmark");
        }

        @GetMapping("/bad-request")
        void badRequest() {
            throw new IllegalArgumentException("bad input");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("resource-not-found maps to 404 with the uniform error body")
    void notFoundShape() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("no such mark"));
    }

    @Test
    @DisplayName("duplicate maps to 409 with the uniform error body")
    void conflictShape() throws Exception {
        mockMvc.perform(get("/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("duplicate bookmark"));
    }

    @Test
    @DisplayName("illegal argument maps to 400 with the uniform error body")
    void badRequestShape() throws Exception {
        mockMvc.perform(get("/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("bad input"));
    }
}
