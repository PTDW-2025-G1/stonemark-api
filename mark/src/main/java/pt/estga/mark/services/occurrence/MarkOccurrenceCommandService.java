package pt.estga.mark.services.occurrence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkOccurrenceRequestDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.services.evidence.MarkEvidenceCommandService;
import pt.estga.mark.services.mark.MarkQueryService;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.mark.entities.Mark;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.shared.enums.ValidationState;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkOccurrenceCommandService {

    private final MarkOccurrenceRepository repository;
    private final MarkOccurrenceMapper mapper;
    private final MarkQueryService markQueryService;
    private final MonumentRepository monumentRepository;
    private final MarkEvidenceRepository evidenceRepository;
    private final MarkEvidenceCommandService evidenceCommandService;

    @Transactional
    public MarkOccurrence create(MarkOccurrence occurrence) {
        return repository.save(occurrence);
    }

    public MarkOccurrence create(MarkOccurrenceRequestDto dto) {
        MarkOccurrence entity = mapper.toEntity(dto);
        return repository.save(entity);
    }

    @Transactional
    public MarkOccurrence update(MarkOccurrence occurrence) {
        if (occurrence.getId() == null) {
            throw new ResourceNotFoundException("Occurrence id must not be null for update");
        }

        MarkOccurrence existing = repository.findById(occurrence.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkOccurrence with id " + occurrence.getId() + " not found"));

        mapper.updateEntityFromDto(mapper.toDto(occurrence), existing);

        return repository.save(existing);
    }

    public MarkOccurrence update(Long id, MarkOccurrenceRequestDto dto) {
        MarkOccurrence existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkOccurrence with id " + id + " not found"));

        mapper.updateFromRequest(dto, existing);

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        MarkOccurrence existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkOccurrence with id " + id + " not found"));

        repository.softDelete(existing);
    }

    /**
     * Ensure an occurrence exists linking mark and monument. If it does not exist, create it.
     * The evidenceId parameter is currently ignored by this placeholder implementation;
     * production code should attach the evidence to the occurrence.
     */
    @Transactional
    public void ensureExists(Long markId, Long monumentId, Long evidenceId) {
        // Single existence check is sufficient
        if (repository.findByMarkIdAndMonumentId(markId, monumentId).isPresent()) return;

        Mark mark = markQueryService.findById(markId).orElseThrow(() -> new IllegalArgumentException("Mark not found"));
        Monument monument = monumentRepository.findById(monumentId).orElseThrow(() -> new IllegalArgumentException("Monument not found"));

        MarkOccurrence occ = MarkOccurrence.builder()
                .mark(mark)
                .monument(monument)
                .build();

        create(occ);
    }

    /**
     * Link a review's evidence (by media file id) to an occurrence, creating the occurrence
     * if necessary and setting its validation state according to approval.
     */
    @Transactional
    public void linkReviewToOccurrence(UUID mediaFileId, Long markId, Long monumentId, boolean approved) {
        if (markId == null || monumentId == null) return;

        MarkOccurrence occurrence = repository.findByMarkIdAndMonumentId(markId, monumentId)
                .orElseGet(() -> {
                    ValidationState state = approved ? ValidationState.VERIFIED : ValidationState.PROVISIONAL;
                    var mark = markQueryService.findById(markId).orElseThrow(() -> new IllegalArgumentException("Mark not found"));
                    var monument = monumentRepository.findById(monumentId).orElseThrow(() -> new IllegalArgumentException("Monument not found"));

                    MarkOccurrence occ = MarkOccurrence.builder()
                            .mark(mark)
                            .monument(monument)
                            .validationState(state)
                            .build();

                    return create(occ);
                });

        if (mediaFileId == null) return;

        try {
            evidenceRepository.findByFileId(mediaFileId).ifPresent(ev -> {
                ev.setOccurrence(occurrence);
                evidenceCommandService.update(ev);
            });
        } catch (Exception e) {
            log.error("Failed to link evidence for file {}: {}", mediaFileId, e.getMessage(), e);
        }
    }
}
