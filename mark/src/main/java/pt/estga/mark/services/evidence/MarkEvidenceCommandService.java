package pt.estga.mark.services.evidence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.repositories.MediaFileRepository;
import pt.estga.mark.dtos.MarkEvidenceRequestDto;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkEvidenceCommandService {

    private final MarkEvidenceRepository repository;
    private final MarkEvidenceMapper mapper;
    private final MediaFileRepository fileRepository;
    private final MarkOccurrenceRepository occurrenceRepository;

    public MarkEvidence create(MarkEvidence evidence) {
        return repository.save(evidence);
    }

    public MarkEvidence create(MarkEvidenceRequestDto dto) {
        MarkEvidence entity = mapper.toEntityFromRequest(dto);

        if (dto.fileId() != null) {
            MediaFile file = fileRepository.findById(dto.fileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Media file not found"));
            entity.setFile(file);
        }

        if (dto.occurrenceId() != null) {
            MarkOccurrence occurrence = occurrenceRepository.findById(dto.occurrenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));
            entity.setOccurrence(occurrence);
        }

        return repository.save(entity);
    }

    public MarkEvidence update(MarkEvidence evidence) {
        if (evidence.getId() == null) {
            throw new ResourceNotFoundException("Evidence id must not be null for update");
        }

        MarkEvidence existing = repository.findById(evidence.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkEvidence with id " + evidence.getId() + " not found"));

        mapper.updateEntityFromDto(mapper.toDto(evidence), existing);

        return repository.save(existing);
    }

    public MarkEvidence update(UUID id, MarkEvidenceRequestDto dto) {
        MarkEvidence existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkEvidence with id " + id + " not found"));

        mapper.updateFromRequest(dto, existing);

        if (dto.fileId() != null) {
            MediaFile file = fileRepository.findById(dto.fileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Media file not found"));
            existing.setFile(file);
        } else {
            existing.setFile(null);
        }

        if (dto.occurrenceId() != null) {
            MarkOccurrence occurrence = occurrenceRepository.findById(dto.occurrenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));
            existing.setOccurrence(occurrence);
        } else {
            existing.setOccurrence(null);
        }

        return repository.save(existing);
    }

    public void deleteById(UUID id) {
        MarkEvidence evidence = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkEvidence with id " + id + " not found"));

        repository.softDelete(evidence);
    }
}
