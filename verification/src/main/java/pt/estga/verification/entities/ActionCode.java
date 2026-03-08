package pt.estga.verification.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.user.entities.User;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Instant;

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

    private String code;

    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private ActionCodeType type;

    @ManyToOne
    private User user;

    // Optional explicit recipient (email/phone) used by dispatch flows.
    private String recipient;

    private String telegramId;

    private boolean consumed;

}
