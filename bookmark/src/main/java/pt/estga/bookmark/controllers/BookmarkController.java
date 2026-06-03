package pt.estga.bookmark.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.bookmark.dto.BookmarkCreateRequest;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.services.BookmarkService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Endpoints for user bookmarks.")
public class BookmarkController {

    private final BookmarkService service;

    @GetMapping("/user/{userId}")
    @Operation(summary = "List bookmarks for a user with pagination.")
    public ResponseEntity<Page<BookmarkResponse>> listByUser(
            @PathVariable Long userId,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listByUser(userId, pageable));
    }

    @PostMapping("/user/{userId}")
    @Operation(summary = "Create a bookmark.")
    public ResponseEntity<BookmarkResponse> create(@PathVariable Long userId, @RequestBody BookmarkCreateRequest request) {
        return ResponseEntity.ok(service.create(userId, request));
    }

    @DeleteMapping("/user/{userId}/{bookmarkId}")
    @Operation(summary = "Delete a bookmark.")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable UUID bookmarkId) {
        service.delete(userId, bookmarkId);
        return ResponseEntity.noContent().build();
    }
}
