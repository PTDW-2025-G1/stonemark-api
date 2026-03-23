package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.services.MarkEvidenceService;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final MarkEvidenceService markEvidenceService;


}
