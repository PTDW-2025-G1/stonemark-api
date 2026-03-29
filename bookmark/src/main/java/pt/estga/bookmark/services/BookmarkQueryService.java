package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
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
		List<BookmarkResponse> monuments = monumentRepo.findAllByUserId(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MONUMENT))
				.collect(Collectors.toList());

		List<BookmarkResponse> marks = markRepo.findAllByUserId(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MARK))
				.toList();

		List<BookmarkResponse> occurrences = markOccurrenceRepo.findAllByUserId(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MARK_OCCURRENCE))
				.toList();

		List<BookmarkResponse> evidences = markEvidenceRepo.findAllByUserId(userId).stream()
				.map(b -> toResponse(b, BookmarkTargetType.MARK_EVIDENCE))
				.toList();

		monuments.addAll(marks);
		monuments.addAll(occurrences);
		monuments.addAll(evidences);

		return monuments;
	}

	public boolean existsByUserAndTarget(Long userId, BookmarkTargetType type, String targetId) {
		return switch (type) {
			case MONUMENT -> monumentRepo.findAllByUserId(userId).stream().anyMatch(b -> b.getMonument().getId().toString().equals(targetId));
			case MARK -> markRepo.findAllByUserId(userId).stream().anyMatch(b -> b.getMark().getId().toString().equals(targetId));
			case MARK_OCCURRENCE -> markOccurrenceRepo.findAllByUserId(userId).stream().anyMatch(b -> b.getMarkOccurrence().getId().toString().equals(targetId));
			case MARK_EVIDENCE -> markEvidenceRepo.findAllByUserId(userId).stream().anyMatch(b -> b.getMarkEvidence().getId().toString().equals(targetId));
			default -> false;
		};
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
