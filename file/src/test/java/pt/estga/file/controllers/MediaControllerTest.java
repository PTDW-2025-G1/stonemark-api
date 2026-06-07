package pt.estga.file.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.application.MediaService;
import pt.estga.file.services.application.MediaVariantService;
import pt.estga.commonweb.exceptions.FileNotFoundException;
import pt.estga.commonweb.exceptions.GlobalExceptionHandler;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    private final MediaService mediaService = mock();
    private final MediaVariantService mediaVariantService = mock();

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new MediaController(mediaService, mediaVariantService)
    ).setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("should return 201 on successful upload")
    void shouldUploadFile() throws Exception {
        UUID fileId = UUID.randomUUID();
        var mockFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{0x00, 0x01});

        var mediaFile = new MediaFile();
        mediaFile.setId(fileId);
        when(mediaService.upload(any(), anyString(), anyLong())).thenReturn(mediaFile);

        mockMvc.perform(multipart("/api/v1/public/media")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(fileId.toString()));
    }

    @Test
    @DisplayName("should return 404 on missing file")
    void shouldReturn404ForMissingFile() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaService.findById(id)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/public/media/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should return 204 on successful delete")
    void shouldDeleteFile() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(mediaService).deleteMedia(id);

        mockMvc.perform(delete("/api/v1/public/media/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 500 when delete fails")
    void shouldReturn500OnDeleteError() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new FileNotFoundException("not found")).when(mediaService).deleteMedia(id);

        mockMvc.perform(delete("/api/v1/public/media/{id}", id))
                .andExpect(status().isNotFound());
    }
}
