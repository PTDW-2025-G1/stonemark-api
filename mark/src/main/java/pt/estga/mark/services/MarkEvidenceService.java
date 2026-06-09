package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkEvidenceService {

    private final MarkEvidenceRepository repository;

    public MarkEvidenceDto findById(UUID id) {
        return repository.findById(id)
                .map(MarkEvidenceMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark evidence with id " + id + " not found"));
    }

    @Transactional
    public void deleteById(UUID id) {
        MarkEvidence evidence = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark evidence with id " + id + " not found"));
        repository.softDelete(evidence);
    }
}
