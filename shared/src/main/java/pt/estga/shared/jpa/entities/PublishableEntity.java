package pt.estga.shared.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import pt.estga.shared.core.enums.PublicationStatus;

import java.time.Instant;

/**
 * Base for publishable, audited, soft-deletable entities.
 *
 * <p>Carries a single lifecycle ({@link PublicationStatus}) plus {@code publishedAt}.
 * Rows are never physically removed in normal operation: soft deletion
 * ({@code deletedAt}) keeps them so database foreign keys remain satisfied, while
 * reads filter deleted rows out.
 */
@MappedSuperclass
@Getter
public abstract class PublishableEntity extends AuditedEntity implements SoftDeletable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    protected PublicationStatus status = PublicationStatus.DRAFT;

    protected Instant publishedAt;

    protected Instant deletedAt;

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

    @Override
    public void markDeleted() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    @Override
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
