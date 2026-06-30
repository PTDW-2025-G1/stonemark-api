package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkQueryServiceImpl implements MarkQueryService {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository occurrenceRepository;
    private final MarkEvidenceRepository evidenceRepository;

    @Override
    public Optional<MarkDto> findMarkById(Long id) {
        return markRepository.findById(id).map(MarkMapper::toDto);
    }

    @Override
    public Optional<MarkOccurrenceDto> findOccurrenceById(Long id) {
        return occurrenceRepository.findById(id).map(MarkOccurrenceMapper::toDto);
    }

    @Override
    public List<MarkEvidenceDto> findEvidenceByIds(List<UUID> ids) {
        return evidenceRepository.findAllById(ids).stream()
                .map(MarkEvidenceMapper::toDto)
                .toList();
    }
}
