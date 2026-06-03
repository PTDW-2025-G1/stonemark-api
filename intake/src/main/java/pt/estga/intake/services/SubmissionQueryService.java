package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.dtos.SubmissionDto;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.mappers.SubmissionMapper;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionQueryService {

    private final MarkEvidenceSubmissionRepository submissionRepository;

    public Page<SubmissionDto> findAll(Pageable pageable, SubmissionStatus status) {
        Specification<MarkEvidenceSubmission> spec = (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
        return submissionRepository.findAll(spec, pageable).map(SubmissionMapper::toDto);
    }

    public Optional<SubmissionDto> findById(Long id) {
        return submissionRepository.findById(id).map(SubmissionMapper::toDto);
    }
}
