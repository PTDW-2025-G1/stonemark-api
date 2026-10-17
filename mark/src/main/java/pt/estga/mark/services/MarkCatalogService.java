package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.PublicMarkDto;
import pt.estga.mark.dtos.PublicMarkSummaryDto;
import pt.estga.mark.dtos.PublicOccurrenceDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.PublicMarkMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read side of the public catalogue. Only published, non-deleted marks and
 * occurrences are returned; everything else stays admin-only.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkCatalogService {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository occurrenceRepository;
    private final MarkEvidenceRepository evidenceRepository;

    public Page<PublicMarkSummaryDto> search(String q, Long monumentId, Boolean located, Pageable pageable) {
        Specification<Mark> spec = published()
                .and(matchesQuery(q))
                .and(atMonument(monumentId))
                .and(located(located));
        return markRepository.findAll(spec, pageable).map(PublicMarkMapper::toSummary);
    }

    public PublicMarkDto findPublished(Long id) {
        Mark mark = markRepository.findByIdAndStatus(id, PublicationStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Mark " + id + " not found"));
        List<MarkOccurrence> occurrences = occurrenceRepository.findByMarkIdAndStatus(id, PublicationStatus.PUBLISHED);
        Map<Long, List<UUID>> mediaByOccurrence = mediaByOccurrence(occurrences);
        List<PublicOccurrenceDto> occurrenceDtos = occurrences.stream()
                .map(occurrence -> PublicMarkMapper.toOccurrence(
                        occurrence, mediaByOccurrence.getOrDefault(occurrence.getId(), List.of())))
                .toList();
        return PublicMarkMapper.toDetail(mark, occurrenceDtos);
    }

    private Map<Long, List<UUID>> mediaByOccurrence(List<MarkOccurrence> occurrences) {
        List<Long> occurrenceIds = occurrences.stream().map(MarkOccurrence::getId).toList();
        if (occurrenceIds.isEmpty()) {
            return Map.of();
        }
        return evidenceRepository.findByOccurrenceIdIn(occurrenceIds).stream()
                .filter(evidence -> evidence.getOccurrence() != null && evidence.getFileId() != null)
                .collect(Collectors.groupingBy(
                        evidence -> evidence.getOccurrence().getId(),
                        Collectors.mapping(MarkEvidence::getFileId, Collectors.toList())));
    }

    private static Specification<Mark> published() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PublicationStatus.PUBLISHED),
                cb.isNull(root.get("deletedAt")));
    }

    private static Specification<Mark> matchesQuery(String q) {
        if (q == null || q.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + q.toLowerCase() + "%");
    }

    private static Specification<Mark> atMonument(Long monumentId) {
        if (monumentId == null) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            var occurrence = root.join("occurrences");
            return cb.and(
                    cb.equal(occurrence.get("monumentId"), monumentId),
                    cb.equal(occurrence.get("status"), PublicationStatus.PUBLISHED),
                    cb.isNull(occurrence.get("deletedAt")));
        };
    }

    private static Specification<Mark> located(Boolean located) {
        if (located == null) return null;
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var occurrence = subquery.from(MarkOccurrence.class);
            subquery.select(occurrence.get("id")).where(
                    cb.equal(occurrence.get("mark"), root),
                    cb.equal(occurrence.get("status"), PublicationStatus.PUBLISHED),
                    cb.isNull(occurrence.get("deletedAt")),
                    located ? cb.isNotNull(occurrence.get("location")) : cb.isNull(occurrence.get("location")));
            return cb.exists(subquery);
        };
    }
}
