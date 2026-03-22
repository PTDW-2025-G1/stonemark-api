package pt.estga.report.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pt.estga.shared.audit.CreationAuditedEntity;
import pt.estga.content.enums.TargetType;
import pt.estga.report.enums.ReportReason;
import pt.estga.report.enums.ReportStatus;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "report",
        indexes = {
                @Index(name = "idx_target_type", columnList = "target_id,target_type")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Report extends CreationAuditedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

}
