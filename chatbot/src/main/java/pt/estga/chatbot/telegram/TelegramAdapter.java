package pt.estga.chatbot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.models.text.TextNode;
import pt.estga.chatbot.models.ui.Button;
import pt.estga.chatbot.models.ui.ContactRequest;
import pt.estga.chatbot.models.ui.LocationRequest;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.models.ui.PhotoItem;
import pt.estga.chatbot.models.ui.TextMessage;
import pt.estga.chatbot.models.ui.UIComponent;
import pt.estga.chatbot.telegram.services.TelegramFileService;
import pt.estga.chatbot.telegram.services.TelegramTextService;
import pt.estga.shared.models.Location;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramAdapter {

    private static final String IMAGE_MIME_TYPE_PREFIX = "image/";

    private final TelegramFileService fileService;
    private final TelegramTextService textService;

    public BotInput toBotInput(Update update) {
        log.debug("Converting Update to BotInput: {}", update);
        if (update.hasMessage()) {
            return handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update.getCallbackQuery());
        }
        log.warn("Update type not supported or empty: {}", update);
        return null;
    }

    private BotInput handleMessage(Message message) {
        long chatId = message.getChatId();
        String userId = String.valueOf(message.getFrom().getId());
        Locale locale = getLocale(message.getFrom().getLanguageCode());

        if (isImageDocument(message)) {
            log.debug("Update contains an image document");
            return createImageInput(chatId, message.getDocument());
        } else if (message.hasPhoto()) {
            log.debug("Update contains a photo");
            return createImageInput(chatId, message.getPhoto());
        } else if (message.hasText()) {
            log.debug("Update contains text message: {}", message.getText());
            return createBotInput(chatId, userId, BotInput.InputType.TEXT, message.getText(), locale);
        } else if (message.hasContact()) {
            log.debug("Update contains contact: {}", message.getContact().getPhoneNumber());
            return createBotInput(chatId, userId, BotInput.InputType.CONTACT, message.getContact().getPhoneNumber(), locale);
        } else if (message.hasLocation()) {
            log.debug("Update contains location");
            return createLocationInput(chatId, userId, message.getLocation(), locale);
        }
        return null;
    }

    private BotInput handleCallbackQuery(CallbackQuery callbackQuery) {
        log.debug("Update contains callback query: {}", callbackQuery.getData());
        return BotInput.builder()
                .userId(String.valueOf(callbackQuery.getFrom().getId()))
                .chatId(callbackQuery.getMessage().getChatId())
                .platform(Platform.TELEGRAM)
                .type(BotInput.InputType.CALLBACK)
                .callbackData(callbackQuery.getData())
                .build();
    }

    private boolean isImageDocument(Message message) {
        return message.hasDocument() &&
                message.getDocument().getMimeType() != null &&
                message.getDocument().getMimeType().startsWith(IMAGE_MIME_TYPE_PREFIX);
    }

    private BotInput createBotInput(long chatId, String userId, BotInput.InputType type, String text, Locale locale) {
        return BotInput.builder()
                .userId(userId)
                .chatId(chatId)
                .platform(Platform.TELEGRAM)
                .type(type)
                .text(text)
                .locale(locale)
                .build();
    }

    private BotInput createLocationInput(long chatId, String userId, org.telegram.telegrambots.meta.api.objects.Location location, Locale locale) {
        return BotInput.builder()
                .userId(userId)
                .chatId(chatId)
                .platform(Platform.TELEGRAM)
                .locale(locale)
                .type(BotInput.InputType.LOCATION)
                .location(new Location(location.getLatitude(), location.getLongitude()))
                .build();
    }

    private Locale getLocale(String languageCode) {
        return languageCode != null ? Locale.forLanguageTag(languageCode) : Locale.getDefault();
    }

    private BotInput createImageInput(long chatId, List<PhotoSize> photos) {
        return photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .map(photo -> processImageFile(chatId, photo.getFileId(), photo.getFileId() + ".jpg"))
                .orElseGet(() -> {
                    log.warn("Received a photo message with no photos for chat: {}", chatId);
                    return null;
                });
    }

    private BotInput createImageInput(long chatId, Document document) {
        return processImageFile(chatId, document.getFileId(), document.getFileName());
    }

    private BotInput processImageFile(long chatId, String fileId, String fileName) {
        try {
            log.debug("Processing image file with ID: {} and name: {}", fileId, fileName);
            File downloadedFile = fileService.downloadFile(fileId);
            if (downloadedFile == null || !downloadedFile.exists()) {
                log.error("Failed to download file or file does not exist. File ID: {}", fileId);
                return null;
            }
            byte[] photoData = Files.readAllBytes(downloadedFile.toPath());
            log.debug("Successfully read {} bytes from file {}", photoData.length, fileName);

            return BotInput.builder()
                    .userId(String.valueOf(chatId))
                    .chatId(chatId)
                    .platform(Platform.TELEGRAM)
                    .type(BotInput.InputType.PHOTO)
                    .fileData(photoData)
                    .fileName(fileName)
                    .build();
        } catch (IOException e) {
            log.error("Error processing image file with ID: {}", fileId, e);
            return null;
        }
    }

    public List<PartialBotApiMethod<?>> toBotApiMethod(long chatId, BotResponse response) {
        log.debug("Converting BotResponse to BotApiMethods for chat: {}", chatId);
        List<PartialBotApiMethod<?>> methods = new ArrayList<>();
        UIComponent uiComponent = response.getUiComponent();
        String chatIdStr = String.valueOf(chatId);

        if (uiComponent instanceof Menu menu) {
            log.debug("Rendering Menu component");
            methods.add(renderMenu(chatIdStr, menu, response.getTextNode()));
            return methods;
        }

        addTextResponse(methods, chatIdStr, response);

        if (uiComponent != null) {
            addUiComponent(methods, chatIdStr, uiComponent);
        }

        return methods;
    }

    private void addTextResponse(List<PartialBotApiMethod<?>> methods, String chatId, BotResponse response) {
        if (response.getTextNode() != null) {
            log.debug("Rendering text node response");
            RenderedText rendered = textService.render(response.getTextNode());
            SendMessage msg = new SendMessage(chatId, rendered.text());
            if (rendered.parseMode() != null) {
                msg.setParseMode(rendered.parseMode());
            }
            methods.add(msg);
        }
    }

    private void addUiComponent(List<PartialBotApiMethod<?>> methods, String chatId, UIComponent uiComponent) {
        if (uiComponent instanceof LocationRequest locationRequest) {
            log.debug("Rendering LocationRequest component");
            methods.add(renderLocationRequest(chatId, locationRequest));
        } else if (uiComponent instanceof ContactRequest contactRequest) {
            log.debug("Rendering ContactRequest component");
            methods.add(renderContactRequest(chatId, contactRequest));
        } else if (uiComponent instanceof PhotoItem photoItem) {
            log.debug("Rendering PhotoItem component");
            methods.addAll(renderPhotoItem(chatId, photoItem));
        } else if (uiComponent instanceof TextMessage textMessage) {
            log.debug("Rendering TextMessage component");
            RenderedText rendered = textService.render(textMessage.getTextNode());
            SendMessage msg = new SendMessage(chatId, rendered.text());
            if (rendered.parseMode() != null) msg.setParseMode(rendered.parseMode());
            methods.add(msg);
        }
    }

    private SendMessage renderMenu(String chatId, Menu menu, TextNode textNode) {
        // Render the provided textNode if it exists, otherwise fallback to the menu title
        RenderedText rendered = textNode != null
                ? textService.render(textNode)
                : textService.render(menu.getTitleNode());

        SendMessage sendMessage = new SendMessage(chatId, rendered.text());
        if (rendered.parseMode() != null) sendMessage.setParseMode(rendered.parseMode());

        if (menu.getButtons() != null && !menu.getButtons().isEmpty()) {
            sendMessage.setReplyMarkup(createInlineKeyboardMarkup(menu.getButtons()));
        }
        return sendMessage;
    }

    private SendMessage renderLocationRequest(String chatId, LocationRequest locationRequest) {
        log.debug("Parsed messageNode for location request");
        RenderedText rendered = textService.render(locationRequest.getMessageNode());
        SendMessage message = new SendMessage(chatId, rendered.text());
        if (rendered.parseMode() != null) message.setParseMode(rendered.parseMode());
        message.setReplyMarkup(createReplyKeyboardMarkup("Send my location", true, false));
        return message;
    }

    private SendMessage renderContactRequest(String chatId, ContactRequest contactRequest) {
        RenderedText rendered = textService.render(contactRequest.getMessageNode());
        SendMessage message = new SendMessage(chatId, rendered.text());
        if (rendered.parseMode() != null) message.setParseMode(rendered.parseMode());
        message.setReplyMarkup(createReplyKeyboardMarkup("Share my phone number", false, true));
        return message;
    }

    private List<PartialBotApiMethod<?>> renderPhotoItem(String chatId, PhotoItem item) {
        List<PartialBotApiMethod<?>> methods = new ArrayList<>();
        RenderedText captionRendered = textService.render(item.getCaptionNode());
        String caption = captionRendered.text();

        if (item.getMediaFileId() != null) {
            InputFile inputFile = fileService.createInputFileFromMediaId(item.getMediaFileId());
            if (inputFile != null) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(chatId);
                photo.setCaption(caption);
                if (captionRendered.parseMode() != null) photo.setParseMode(captionRendered.parseMode());
                photo.setPhoto(inputFile);
                methods.add(photo);
                return methods;
            }
            log.warn("Media file ID {} could not be resolved to an InputFile", item.getMediaFileId());
            caption += " (Image unavailable)";
        }

        SendMessage msg = new SendMessage(chatId, caption);
        if (captionRendered.parseMode() != null) msg.setParseMode(captionRendered.parseMode());
        methods.add(msg);
        return methods;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<List<Button>> buttons) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (List<Button> row : buttons) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            for (Button button : row) {
                InlineKeyboardButton inlineButton = new InlineKeyboardButton();
                RenderedText rendered = textService.render(button.getTextNode());
                inlineButton.setText(rendered.text());
                inlineButton.setCallbackData(button.getCallbackData());
                rowInline.add(inlineButton);
            }
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }


    private ReplyKeyboardMarkup createReplyKeyboardMarkup(String buttonText, boolean requestLocation, boolean requestContact) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton();
        button.setText(buttonText);
        if (requestLocation) button.setRequestLocation(true);
        if (requestContact) button.setRequestContact(true);
        row.add(button);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setOneTimeKeyboard(true);
        return keyboardMarkup;
    }
}
