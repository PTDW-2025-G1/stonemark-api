package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.core.utils.GeometryUtils;

import java.util.Collection;
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
    public List<MarkDto> findDraftMarks() {
        return markRepository.findByStatus(PublicationStatus.DRAFT).stream()
                .map(MarkMapper::toDto)
                .toList();
    }

    @Override
    public List<MarkDto> findPublishedMarksByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return markRepository.findByIdInAndStatusAndDeletedAtIsNull(ids, PublicationStatus.PUBLISHED).stream()
                .map(MarkMapper::toDto)
                .toList();
    }

    @Override
    public List<MarkOccurrenceDto> findPublishedOccurrencesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return occurrenceRepository.findByIdInAndStatusAndDeletedAtIsNull(ids, PublicationStatus.PUBLISHED).stream()
                .map(MarkOccurrenceMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public MarkDto createMark(String title, String description) {
        Mark mark = markRepository.save(Mark.builder().title(title).description(description).build());
        return MarkMapper.toDto(mark);
    }

    @Override
    @Transactional
    public MarkOccurrenceDto findOrCreateOccurrence(Long markId, Long monumentId, Double latitude, Double longitude) {
        MarkOccurrence occurrence = monumentId != null
                ? occurrenceRepository.findByMarkIdAndMonumentId(markId, monumentId)
                        .orElseGet(() -> createOccurrence(markId, monumentId, null))
                : createOccurrence(markId, null, GeometryUtils.createPoint(latitude, longitude));
        return MarkOccurrenceMapper.toDto(occurrence);
    }

    private MarkOccurrence createOccurrence(Long markId, Long monumentId, Point location) {
        Mark mark = markRepository.findById(markId)
                .orElseThrow(() -> new IllegalArgumentException("Mark not found: " + markId));
        return occurrenceRepository.save(MarkOccurrence.builder()
                .mark(mark)
                .monumentId(monumentId)
                .location(location)
                .build());
    }

    @Override
    @Transactional
    public void linkFileToOccurrence(UUID fileId, Long occurrenceId, Long submissionId, float[] embedding) {
        MarkOccurrence occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence not found: " + occurrenceId));

        evidenceRepository.findByFileId(fileId).ifPresentOrElse(ev -> {
            ev.setOccurrence(occurrence);
            ev.setSubmissionId(submissionId);
            evidenceRepository.save(ev);
        }, () -> {
            MarkEvidence newEvidence = MarkEvidence.builder()
                    .fileId(fileId)
                    .occurrence(occurrence)
                    .submissionId(submissionId)
                    .embedding(embedding)
                    .build();
            evidenceRepository.save(newEvidence);
        });
    }
}
