package pt.estga.report.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.report.dtos.ReportRequestDto;
import pt.estga.report.dtos.ReportResponseDto;
import pt.estga.report.entities.Report;
import pt.estga.report.enums.ReportStatus;
import pt.estga.report.mappers.ReportMapper;
import pt.estga.report.repositories.ReportRepository;
import pt.estga.shared.filters.FilterNode;
import pt.estga.shared.filters.SpecificationBuilder;
import pt.estga.user.entities.User;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository repository;
    private final ReportMapper mapper;
    private final SpecificationBuilder<Report> specificationBuilder;

    @Transactional(readOnly = true)
    public Page<Report> search(FilterNode filter, Pageable pageable) {
        return repository.findAll(specificationBuilder.build(filter), pageable);
    }

    @Transactional
    public ReportResponseDto createReport(User user, ReportRequestDto dto) {

        repository.findByCreatedByIdAndTargetIdAndTargetType(
                user.getId(),
                dto.targetId(),
                dto.targetType()
        ).ifPresent(existing -> {
            throw new IllegalStateException("Report already exists for this target");
        });

        Report report = mapper.toEntity(dto);
        report.setStatus(ReportStatus.PENDING);

        repository.save(report);
        return mapper.toDto(report);
    }

    @Transactional
    public ReportResponseDto updateStatus(Long reportId, ReportStatus status) {
        Report report = repository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.setStatus(status);
        return mapper.toDto(report);
    }
}
