package pt.estga.chatbot.telegram.services;

import jakarta.inject.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;
import pt.estga.file.controllers.MediaController;

import java.io.File;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramFileService {

    private final Provider<StonemarkTelegramBot> botProvider;
    private final MediaController mediaController;

    public File downloadFile(String fileId) {
        try {
            StonemarkTelegramBot bot = botProvider.get();
            org.telegram.telegrambots.meta.api.objects.File file = bot.execute(new GetFile(fileId));
            return bot.downloadFile(file);
        } catch (TelegramApiException e) {
            log.error("Error downloading file with ID: {}", fileId, e);
            return null;
        }
    }

    public InputFile createInputFileFromMediaId(UUID mediaId) {
        InputFile inputFile = new InputFile();
        try {
            var responseEntity = mediaController.getMediaById(mediaId);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                inputFile.setMedia(responseEntity.getBody().getInputStream(), "image.jpg");
                return inputFile;
            } else {
                log.warn("Could not retrieve media with ID {} from MediaController", mediaId);
            }
        } catch (Exception e) {
            log.error("Error retrieving media from controller for ID: {}", mediaId, e);
        }
        return null;
    }
}
