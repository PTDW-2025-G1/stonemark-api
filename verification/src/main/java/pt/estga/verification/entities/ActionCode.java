package pt.estga.verification.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Instant;

/**
 * Verification code entity for chatbot account linking.
 * Used to verify ownership of chatbot accounts (e.g., Telegram).
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ActionCode {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionCodeType type;

    @Column(nullable = false)
    private String telegramId;

    @Column(nullable = false)
    private boolean consumed;

}
