package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.enums.EntityStatus;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkService {

    private final MarkRepository repository;
    private final MarkEvidenceRepository evidenceRepository;

    public Page<MarkDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public MarkDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
    }

    @Transactional
    public MarkDto update(Long id, MarkUpdateDto dto) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        mark.setTitle(dto.title());
        mark.setDescription(dto.description());
        if (dto.goldenExampleId() != null) {
            MarkEvidence golden = evidenceRepository.findById(dto.goldenExampleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Evidence with id " + dto.goldenExampleId() + " not found"));
            mark.setGoldenExample(golden);
        }
        if (dto.active() != null) {
            if (dto.active()) {
                mark.activate();
            } else {
                mark.deactivate();
            }
        }
        return toDto(repository.save(mark));
    }

    @Transactional
    public void deleteById(Long id) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        repository.softDelete(mark);
    }

    private MarkDto toDto(Mark mark) {
        MarkEvidence golden = mark.getGoldenExample();
        return new MarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                golden != null ? golden.getEmbedding() : null,
                golden != null ? golden.getFileId() : null,
                mark.getStatus() == EntityStatus.ACTIVE
        );
    }
}
