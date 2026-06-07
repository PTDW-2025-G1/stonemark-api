package pt.estga.chatbot.whatsapp;

import lombok.extern.slf4j.Slf4j;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Platform;
import pt.estga.commoncore.models.Location;

@Slf4j
public class WhatsAppMapper {

    public static BotInput toBotMessage(WhatsAppWebhookPayload payload) {
        if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) {
            log.warn("WhatsApp payload or entry is null or empty");
            return null;
        }

        WhatsAppWebhookPayload.Entry entry = payload.getEntry().getFirst();
        if (entry.getChanges() == null || entry.getChanges().isEmpty()) {
            log.warn("WhatsApp changes is null or empty");
            return null;
        }

        WhatsAppWebhookPayload.Change change = entry.getChanges().getFirst();
        if (change.getValue() == null || change.getValue().getMessages() == null || change.getValue().getMessages().isEmpty()) {
            log.warn("WhatsApp value or messages is null or empty");
            return null;
        }

        WhatsAppWebhookPayload.Message message = change.getValue().getMessages().getFirst();
        
        String text = "";
        BotInput.InputType type = BotInput.InputType.TEXT;
        Location location = null;
        String callbackData = null;

        if ("text".equals(message.getType()) && message.getText() != null) {
            text = message.getText().getBody();
        } else if ("image".equals(message.getType()) && message.getImage() != null) {
            type = BotInput.InputType.PHOTO;
            // TODO: Handle image download using message.getImage().getId()
            // For now, we might need to fetch the image URL or content separately
        } else if ("location".equals(message.getType()) && message.getLocation() != null) {
            type = BotInput.InputType.LOCATION;
            location = Location.builder()
                    .latitude(message.getLocation().getLatitude())
                    .longitude(message.getLocation().getLongitude())
                    .build();
        } else if ("interactive".equals(message.getType()) && message.getInteractive() != null) {
            if ("button_reply".equals(message.getInteractive().getType()) && message.getInteractive().getButtonReply() != null) {
                type = BotInput.InputType.CALLBACK;
                callbackData = message.getInteractive().getButtonReply().getId();
                text = message.getInteractive().getButtonReply().getTitle();
            }
        }

        // WhatsApp uses phone number as ID, which is usually a Long compatible string
        long chatId = 0;
        try {
            chatId = Long.parseLong(message.getFrom());
        } catch (NumberFormatException e) {
            log.error("Could not parse chatId from 'from' field: " + message.getFrom(), e);
        }

        return BotInput.builder()
                .chatId(chatId)
                .userId(message.getFrom()) // Using phone number as userId for now
                .platform(Platform.WHATSAPP)
                .type(type)
                .text(text)
                .location(location)
                .callbackData(callbackData)
                .build();
    }
}
