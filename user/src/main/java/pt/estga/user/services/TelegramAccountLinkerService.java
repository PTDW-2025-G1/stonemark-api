package pt.estga.user.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.estga.shared.exceptions.InvalidTelegramTokenException;
import pt.estga.user.entities.User;
import pt.estga.user.enums.ChatbotPlatform;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAccountLinkerService {

    private final ChatbotAccountService chatbotAccountService;
    private final ObjectMapper objectMapper;

    @Value("${telegram.bot.token}")
    private String botToken;

    public void linkAccount(User user, String telegramData) {
        try {
            TelegramUserData userData = objectMapper.readValue(telegramData, TelegramUserData.class);
            if (!isValid(userData)) {
                throw new InvalidTelegramTokenException("Invalid Telegram data.");
            }
            log.info("Linking Telegram account for user {} with Telegram ID {}", user.getUsername(), userData.getId());
            chatbotAccountService.createAndAssociate(user, ChatbotPlatform.TELEGRAM, String.valueOf(userData.getId()));
        } catch (JsonProcessingException e) {
            throw new InvalidTelegramTokenException("Error processing Telegram data.", e);
        }
    }

    private boolean isValid(TelegramUserData userData) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);

            String dataCheckString = "auth_date=" + userData.getAuthDate() + "\n" +
                    "first_name=" + userData.getFirstName() + "\n" +
                    "id=" + userData.getId() + "\n" +
                    "last_name=" + userData.getLastName() + "\n" +
                    "photo_url=" + userData.getPhotoUrl() + "\n" +
                    "username=" + userData.getUsername();

            byte[] hmac = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(userData.getHash());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating Telegram hash", e);
            return false;
        }
    }

    @Data
    private static class TelegramUserData {
        private long id;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
        private String username;
        @JsonProperty("photo_url")
        private String photoUrl;
        @JsonProperty("auth_date")
        private long authDate;
        private String hash;
    }
}
