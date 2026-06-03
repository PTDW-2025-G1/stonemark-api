package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.dtos.SubmissionFilter;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.shared.jpa.SpecBuilder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionQueryService {

    private final MarkEvidenceSubmissionRepository submissionRepository;

    public Page<SubmissionDto> findAll(Pageable pageable, SubmissionStatus status) {
        SubmissionFilter filter = new SubmissionFilter(status, null, null, null, null, null);
        return search(filter, pageable);
    }

    public Page<SubmissionDto> search(SubmissionFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<MarkEvidenceSubmission>()
                .eq("status", filter.status())
                .eq("submissionSource", filter.source())
                .eq("submittedById", filter.submittedById())
                .eq("division.id", filter.divisionId())
                .afterOrEqual("submittedAt", filter.submittedAfter())
                .beforeOrEqual("submittedAt", filter.submittedBefore());
        return submissionRepository.findAll(sb.build(), pageable).map(SubmissionMapper::toDto);
    }

    public Optional<SubmissionDto> findById(Long id) {
        return submissionRepository.findById(id).map(SubmissionMapper::toDto);
    }
}
