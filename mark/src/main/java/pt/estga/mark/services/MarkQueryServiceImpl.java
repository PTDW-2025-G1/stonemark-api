package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.core.utils.GeometryUtils;

import java.util.Collection;
import java.util.Comparator;
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

    /** Occurrences of the same mark within this distance are treated as the same finding. */
    @Value("${mark.occurrence.dedupe-radius-meters:15}")
    private double occurrenceDedupeRadiusMeters;

    @Override
    public Optional<MarkDto> findMarkById(UUID id) {
        return markRepository.findById(id).map(MarkMapper::toDto);
    }

    @Override
    public List<MarkDto> findDraftMarks() {
        return markRepository.findByStatus(PublicationStatus.DRAFT).stream()
                .map(MarkMapper::toDto)
                .toList();
    }

    @Override
    public List<MarkDto> findPublishedMarksByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return markRepository.findByIdInAndStatusAndDeletedAtIsNull(ids, PublicationStatus.PUBLISHED).stream()
                .map(MarkMapper::toDto)
                .toList();
    }

    @Override
    public List<MarkOccurrenceDto> findPublishedOccurrencesByIds(Collection<UUID> ids) {
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
    public MarkOccurrenceDto findOrCreateOccurrence(UUID markId, UUID monumentId, Double latitude, Double longitude) {
        if (monumentId != null) {
            return MarkOccurrenceMapper.toDto(occurrenceRepository.findByMarkIdAndMonumentId(markId, monumentId)
                    .orElseGet(() -> createOccurrence(markId, monumentId, null)));
        }
        if (latitude == null || longitude == null) {
            return MarkOccurrenceMapper.toDto(createOccurrence(markId, null, null));
        }
        return occurrenceRepository.findByMarkIdAndLocationIsNotNull(markId).stream()
                .filter(occurrence -> distanceTo(latitude, longitude, occurrence) <= occurrenceDedupeRadiusMeters)
                .min(Comparator.comparingDouble(occurrence -> distanceTo(latitude, longitude, occurrence)))
                .map(MarkOccurrenceMapper::toDto)
                .orElseGet(() -> MarkOccurrenceMapper.toDto(
                        createOccurrence(markId, null, GeometryUtils.createPoint(latitude, longitude))));
    }

    private static double distanceTo(Double latitude, Double longitude, MarkOccurrence occurrence) {
        Point location = occurrence.getLocation();
        return GeometryUtils.distanceMeters(latitude, longitude, location.getY(), location.getX());
    }

    @Override
    @Transactional
    public void publishOccurrence(UUID occurrenceId) {
        occurrenceRepository.findById(occurrenceId).ifPresent(occurrence -> {
            if (occurrence.getStatus() != PublicationStatus.PUBLISHED) {
                occurrence.publish();
                occurrenceRepository.save(occurrence);
            }
        });
    }

    private MarkOccurrence createOccurrence(UUID markId, UUID monumentId, Point location) {
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
    public void linkFileToOccurrence(UUID fileId, UUID occurrenceId, UUID submissionId, float[] embedding) {
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
