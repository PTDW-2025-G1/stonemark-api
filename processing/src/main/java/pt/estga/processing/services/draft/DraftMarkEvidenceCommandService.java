package pt.estga.processing.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;

@Service
@RequiredArgsConstructor
public class DraftMarkEvidenceCommandService {

    private final DraftMarkEvidenceRepository repository;

}
