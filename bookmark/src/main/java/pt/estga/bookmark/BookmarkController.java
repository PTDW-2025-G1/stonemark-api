package pt.estga.bookmark;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.shared.enums.TargetType;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Endpoints for user bookmarks.")
public class BookmarkController {

    private final BookmarkService service;
    private final BookmarkQueryService queryService;

    @GetMapping
    public List<BookmarkDto> getUserBookmarks(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return queryService.getUserBookmarks(principal.getId());
    }

    @GetMapping("/check/{type}/{targetId}")
    public boolean isBookmarked(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable TargetType type,
            @PathVariable String targetId
    ) {
        return queryService.isBookmarked(principal.getId(), type, targetId);
    }

    @PostMapping("/{type}/{targetId}")
    public BookmarkDto create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable TargetType type,
            @PathVariable String targetId
    ) {
        return service.createBookmark(principal.getId(), type, targetId);
    }

    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long bookmarkId
    ) {
        service.deleteBookmark(principal.getId(), bookmarkId);
        return ResponseEntity.ok().build();
    }
}
