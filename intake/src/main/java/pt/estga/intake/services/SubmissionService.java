package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.dtos.SubmissionFilter;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.commoninfra.jpa.SpecBuilder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final MarkEvidenceSubmissionRepository submissionRepository;

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
}
