package pt.estga.shared.jpa.entities;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

/**
 * Base for audited entities that are removed by marking a timestamp rather than
 * deleting the row, so that referencing rows stay valid.
 */
@MappedSuperclass
@Getter
public abstract class SoftDeletableEntity extends AuditedEntity implements SoftDeletable {

    protected Instant deletedAt;

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
