package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.commoncore.enums.ValidationState;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.MarkOccurrence;
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

    @Override
    @Transactional
    public MarkDto createMark(String title) {
        Mark mark = markRepository.save(Mark.builder().title(title).build());
        return MarkMapper.toDto(mark);
    }

    @Override
    @Transactional
    public MarkOccurrenceDto findOrCreateOccurrence(Long markId, boolean approved) {
        MarkOccurrence occurrence = occurrenceRepository.findByMarkIdAndMonumentIdIsNull(markId)
                .orElseGet(() -> {
                    ValidationState state = approved ? ValidationState.VERIFIED : ValidationState.PROVISIONAL;
                    Mark mark = markRepository.findById(markId)
                            .orElseThrow(() -> new IllegalArgumentException("Mark not found"));
                    return occurrenceRepository.save(MarkOccurrence.builder()
                            .mark(mark)
                            .monumentId(null)
                            .validationState(state)
                            .build());
                });
        return MarkOccurrenceMapper.toDto(occurrence);
    }

    @Override
    @Transactional
    public void linkFileToOccurrence(UUID fileId, Long occurrenceId, float[] embedding) {
        MarkOccurrence occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence not found: " + occurrenceId));

        evidenceRepository.findByFileId(fileId).ifPresentOrElse(ev -> {
            ev.setOccurrence(occurrence);
            evidenceRepository.save(ev);
        }, () -> {
            MarkEvidence newEvidence = MarkEvidence.builder()
                    .fileId(fileId)
                    .occurrence(occurrence)
                    .embedding(embedding)
                    .build();
            evidenceRepository.save(newEvidence);
        });
    }
}
