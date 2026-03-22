package pt.estga.user.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.user.enums.ChatbotPlatform;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChatbotAccount {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChatbotPlatform chatbotPlatform;

    private String value;

    @ManyToOne
    @JoinColumn
    private User user;
}
