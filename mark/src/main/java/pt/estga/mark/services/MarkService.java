package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkService {

    private final MarkRepository repository;
    private final MarkEvidenceRepository evidenceRepository;

    public MarkDto findById(Long id) {
        return repository.findById(id)
                .map(MarkMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
    }

    @Transactional
    public MarkDto update(Long id, MarkUpdateDto dto) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        mark.setTitle(dto.title());
        mark.setDescription(dto.description());
        if (dto.exemplarId() != null) {
            MarkEvidence exemplar = evidenceRepository.findById(dto.exemplarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Evidence with id " + dto.exemplarId() + " not found"));
            mark.setExemplar(exemplar);
        }
        if (dto.active() != null) {
            if (dto.active()) {
                mark.activate();
            } else {
                mark.deactivate();
            }
        }
        return MarkMapper.toDto(repository.save(mark));
    }

    @Transactional
    public void deleteById(Long id) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        repository.softDelete(mark);
    }
}
