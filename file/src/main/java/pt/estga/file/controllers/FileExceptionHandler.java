package pt.estga.file.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pt.estga.file.exceptions.MediaPersistenceException;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.commonweb.dtos.MessageResponseDto;

@Slf4j
@ControllerAdvice(basePackageClasses = MediaController.class)
public class FileExceptionHandler {

    @ExceptionHandler(OversizeFileException.class)
    public ResponseEntity<MessageResponseDto> handleOversizeFile(OversizeFileException ex) {
        log.warn("File upload rejected: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(MessageResponseDto.error(ex.getMessage()));
    }

    @ExceptionHandler(MediaPersistenceException.class)
    public ResponseEntity<MessageResponseDto> handleMediaPersistence(MediaPersistenceException ex) {
        log.error("Media persistence failed: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponseDto.error("Media storage failed"));
    }
}
