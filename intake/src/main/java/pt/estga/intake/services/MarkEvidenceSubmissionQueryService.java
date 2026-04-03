package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

@Service
@RequiredArgsConstructor
public class MarkEvidenceSubmissionQueryService {

    private final MarkEvidenceSubmissionRepository repository;


}
