package pt.estga.report.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.report.dtos.ReportRequestDto;
import pt.estga.report.dtos.ReportResponseDto;
import pt.estga.report.enums.ReportStatus;
import pt.estga.user.entities.User;

public interface ReportService {

    ReportResponseDto createReport(User user, ReportRequestDto dto);

    Page<ReportResponseDto> getAllReports(Pageable pageable);

    ReportResponseDto updateStatus(Long reportId, ReportStatus status);

}
