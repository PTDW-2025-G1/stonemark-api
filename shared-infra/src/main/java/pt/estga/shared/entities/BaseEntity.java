package pt.estga.shared.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import pt.estga.shared.enums.EntityStatus;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class BaseEntity extends AuditedEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected EntityStatus status = EntityStatus.ACTIVE;

    protected Instant deletedAt;

    public void markDeleted() {
        if (this.deletedAt != null) return;
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void activate() {
        this.status = EntityStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = EntityStatus.INACTIVE;
    }

    public void archive() {
        this.status = EntityStatus.ARCHIVED;
    }
}