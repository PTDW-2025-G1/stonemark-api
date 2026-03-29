package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.bookmark.dto.BookmarkCreateRequest;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.entities.MarkBookmark;
import pt.estga.bookmark.entities.MarkEvidenceBookmark;
import pt.estga.bookmark.entities.MarkOccurrenceBookmark;
import pt.estga.bookmark.entities.MonumentBookmark;
import pt.estga.bookmark.repositories.MarkBookmarkRepository;
import pt.estga.bookmark.repositories.MarkEvidenceBookmarkRepository;
import pt.estga.bookmark.repositories.MarkOccurrenceBookmarkRepository;
import pt.estga.bookmark.repositories.MonumentBookmarkRepository;
import pt.estga.bookmark.repositories.BaseBookmarkRepository;
import pt.estga.sharedweb.exceptions.DuplicateResourceException;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookmarkService {

	private final BookmarkQueryService queryService;
	private final MonumentBookmarkRepository monumentRepo;
	private final MarkBookmarkRepository markRepo;
	private final MarkOccurrenceBookmarkRepository markOccurrenceRepo;
	private final MarkEvidenceBookmarkRepository markEvidenceRepo;
    private final BaseBookmarkRepository baseBookmarkRepo;
	private final UserRepository userRepository;

	@Transactional
	public BookmarkResponse create(Long userId, BookmarkCreateRequest request) {
		if (queryService.existsByUserAndTarget(userId, request.targetType(), request.targetId())) {
			throw new DuplicateResourceException("Bookmark already exists");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		switch (request.targetType()) {
			case MONUMENT -> {
				MonumentBookmark b = MonumentBookmark.builder().createdBy(user).build();
				// The caller is expected to set relationship references before saving if needed
				monumentRepo.save(b);
				return queryService.listByUser(userId).stream().filter(r -> r.id().equals(b.getId())).findFirst().orElseThrow();
			}
			case MARK -> {
				MarkBookmark b = MarkBookmark.builder().createdBy(user).build();
				markRepo.save(b);
				return queryService.listByUser(userId).stream().filter(r -> r.id().equals(b.getId())).findFirst().orElseThrow();
			}
			case MARK_OCCURRENCE -> {
				MarkOccurrenceBookmark b = MarkOccurrenceBookmark.builder().createdBy(user).build();
				markOccurrenceRepo.save(b);
				return queryService.listByUser(userId).stream().filter(r -> r.id().equals(b.getId())).findFirst().orElseThrow();
			}
			case MARK_EVIDENCE -> {
				MarkEvidenceBookmark b = MarkEvidenceBookmark.builder().createdBy(user).build();
				markEvidenceRepo.save(b);
				return queryService.listByUser(userId).stream().filter(r -> r.id().equals(b.getId())).findFirst().orElseThrow();
			}
			default -> throw new IllegalArgumentException("Unsupported bookmark type");
		}
	}

	@Transactional
	public void delete(Long userId, UUID bookmarkId) {
		baseBookmarkRepo.findByIdAndCreatedById(bookmarkId, userId)
				.ifPresent(baseBookmarkRepo::delete);
	}
}
