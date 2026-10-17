package pt.estga.shared.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import pt.estga.shared.core.enums.PublicationStatus;

import java.time.Instant;

/**
 * Base for publishable entities: audited content with a lifecycle
 * ({@link PublicationStatus}) and soft deletion inherited from
 * {@link SoftDeletableEntity}.
 */
@MappedSuperclass
@Getter
public abstract class PublishableEntity extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    protected PublicationStatus status = PublicationStatus.DRAFT;

    protected Instant publishedAt;

    /**
     * Moves the entity to {@link PublicationStatus#PUBLISHED}, stamping the first
     * publication time once and preserving it across later transitions.
     */
    public void publish() {
        this.status = PublicationStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
    }

    public void archive() {
        this.status = PublicationStatus.ARCHIVED;
    }

    public void revertToDraft() {
        this.status = PublicationStatus.DRAFT;
    }
}
