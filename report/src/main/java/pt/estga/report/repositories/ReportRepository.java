package pt.estga.report.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.content.enums.TargetType;
import pt.estga.report.entities.Report;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByCreatedByIdAndTargetIdAndTargetType(Long createdById, Long targetId, TargetType targetType);
}
