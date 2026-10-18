package pt.estga.support.entities;

import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import pt.estga.support.enums.ContactStatus;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ContactRequest {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private String name; // todo: first and last name

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message; // todo: add limit

    @CreatedDate
    @Column(nullable = false, updatable = false)
    protected Instant createdAt;

    @Enumerated(EnumType.STRING)
    private ContactStatus status = ContactStatus.PENDING;

    // todo: add processedAt, processedBy

    @Column
    private UUID submittedById;

    // todo: add method isProcessed() and add more states

}
