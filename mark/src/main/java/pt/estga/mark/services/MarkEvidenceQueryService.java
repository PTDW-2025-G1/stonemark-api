package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.repositories.MarkEvidenceRepository;

@Service
@RequiredArgsConstructor
public class MarkEvidenceQueryService {

    private final MarkEvidenceRepository repository;

}
