package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.bookmark.enums.BookmarkTargetType;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.entities.BaseBookmark;
import pt.estga.bookmark.entities.MarkBookmark;
import pt.estga.bookmark.entities.MarkEvidenceBookmark;
import pt.estga.bookmark.entities.MarkOccurrenceBookmark;
import pt.estga.bookmark.entities.MonumentBookmark;
import pt.estga.bookmark.repositories.MarkBookmarkRepository;
import pt.estga.bookmark.repositories.MarkEvidenceBookmarkRepository;
import pt.estga.bookmark.repositories.MarkOccurrenceBookmarkRepository;
import pt.estga.bookmark.repositories.MonumentBookmarkRepository;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.monument.MonumentMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkQueryService {

	private final MonumentBookmarkRepository monumentRepo;
	private final MarkBookmarkRepository markRepo;
	private final MarkOccurrenceBookmarkRepository markOccurrenceRepo;
	private final MarkEvidenceBookmarkRepository markEvidenceRepo;
	private final MonumentMapper monumentMapper;
	private final MarkMapper markMapper;
	private final MarkOccurrenceMapper markOccurrenceMapper;
    private final MarkEvidenceMapper markEvidenceMapper;

	public List<BookmarkResponse> listByUser(Long userId) {
		List<BookmarkResponse> monuments = monumentRepo.findAllByCreatedById(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MONUMENT))
				.collect(Collectors.toList());

		List<BookmarkResponse> marks = markRepo.findAllByCreatedById(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MARK))
				.toList();

		List<BookmarkResponse> occurrences = markOccurrenceRepo.findAllByCreatedById(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MARK_OCCURRENCE))
				.toList();

		List<BookmarkResponse> evidences = markEvidenceRepo.findAllByCreatedById(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MARK_EVIDENCE))
				.toList();

		monuments.addAll(marks);
		monuments.addAll(occurrences);
		monuments.addAll(evidences);

		return monuments;
	}

	public boolean existsByUserAndTarget(Long userId, BookmarkTargetType type, String targetId) {
		try {
			return switch (type) {
				case MONUMENT -> {
					Long mid = Long.parseLong(targetId);
					yield monumentRepo.existsByCreatedByIdAndMonumentId(userId, mid);
				}
				case MARK -> {
					Long mid = Long.parseLong(targetId);
					yield markRepo.existsByCreatedByIdAndMarkId(userId, mid);
				}
				case MARK_OCCURRENCE -> {
					Long occId = Long.parseLong(targetId);
					yield markOccurrenceRepo.existsByCreatedByIdAndMarkOccurrenceId(userId, occId);
				}
				case MARK_EVIDENCE -> {
					// Mark evidence ids are UUIDs
					UUID evidenceUuid = UUID.fromString(targetId);
					yield markEvidenceRepo.existsByCreatedByIdAndMarkEvidenceId(userId, evidenceUuid);
				}
				default -> false;
			};
		} catch (IllegalArgumentException e) {
			log.warn("Invalid target ID format for type {}: {}", type, targetId);
			return false;
		}
	}

	private BookmarkResponse toResponse(BaseBookmark b, BookmarkTargetType type) {
		UUID id = b.getId();
        String targetId;
		Object content = null;
		switch (type) {
			case MONUMENT -> {
				MonumentBookmark mb = (MonumentBookmark) b;
				targetId = String.valueOf(mb.getMonument().getId());
				content = monumentMapper.toResponseDto(mb.getMonument());
			}
			case MARK -> {
				MarkBookmark mb = (MarkBookmark) b;
				targetId = String.valueOf(mb.getMark().getId());
				content = markMapper.toDto(mb.getMark());
			}
			case MARK_OCCURRENCE -> {
				MarkOccurrenceBookmark mb = (MarkOccurrenceBookmark) b;
				targetId = String.valueOf(mb.getMarkOccurrence().getId());
				content = markOccurrenceMapper.toDto(mb.getMarkOccurrence());
			}
			case MARK_EVIDENCE -> {
				MarkEvidenceBookmark mb = (MarkEvidenceBookmark) b;
				targetId = String.valueOf(mb.getMarkEvidence().getId());
				content = markEvidenceMapper.toDto(mb.getMarkEvidence());
			}
			default -> targetId = "";
		}
		return new BookmarkResponse(id, type, targetId, b.getCreatedAt(), content);
	}
}
