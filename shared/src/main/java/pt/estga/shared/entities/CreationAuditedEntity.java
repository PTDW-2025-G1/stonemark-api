package pt.estga.shared.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pt.estga.shared.models.AuditActor;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class CreationAuditedEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    protected Instant createdAt;

    @CreatedBy
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "created_by_id", updatable = false)),
            @AttributeOverride(name = "identifier", column = @Column(name = "created_by_identifier", updatable = false))
    })
    protected AuditActor createdBy;

}
