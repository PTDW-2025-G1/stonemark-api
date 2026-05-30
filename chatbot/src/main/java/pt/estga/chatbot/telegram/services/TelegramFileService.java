package pt.estga.chatbot.telegram.services;

import jakarta.inject.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.storage.FileStorageService;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramFileService {

    private final Provider<StonemarkTelegramBot> botProvider;
    private final MediaMetadataService mediaMetadataService;
    private final FileStorageService fileStorageService;

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
            Resource resource = mediaMetadataService.findById(mediaId)
                    .map(MediaFile::getStoragePath)
                    .map(fileStorageService::loadFile)
                    .orElse(null);

            if (resource != null) {
                try (InputStream is = resource.getInputStream()) {
                    inputFile.setMedia(is, "image.jpg");
                    return inputFile;
                }
            } else {
                log.warn("Could not retrieve media with ID {}", mediaId);
            }
        } catch (Exception e) {
            log.error("Error retrieving media for ID: {}", mediaId, e);
        }
        return null;
    }
}
