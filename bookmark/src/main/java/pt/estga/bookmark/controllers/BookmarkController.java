package pt.estga.bookmark.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pt.estga.bookmark.dto.BookmarkCreateRequest;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.services.BookmarkQueryService;
import pt.estga.bookmark.services.BookmarkService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Endpoints for user bookmarks.")
public class BookmarkController {

	private final BookmarkQueryService queryService;
	private final BookmarkService service;

	@GetMapping("/user/{userId}")
	public List<BookmarkResponse> listByUser(@PathVariable Long userId) {
		return queryService.listByUser(userId);
	}

	@PostMapping("/user/{userId}")
	public BookmarkResponse create(@PathVariable Long userId, @RequestBody BookmarkCreateRequest request) {
		return service.create(userId, request);
	}

	@DeleteMapping("/user/{userId}/{bookmarkId}")
	public void delete(@PathVariable Long userId, @PathVariable UUID bookmarkId) {
		service.delete(userId, bookmarkId);
	}

}
