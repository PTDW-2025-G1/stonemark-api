package pt.estga.bookmark.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.bookmark.dto.BookmarkCreateRequest;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.services.BookmarkService;
import pt.estga.shared.core.interfaces.AuthenticatedPrincipal;

import java.util.UUID;

/**
 * Bookmarks always belong to the authenticated caller: the user is taken from the
 * security context, never from the request, so one user cannot read or modify
 * another user's bookmarks.
 */
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Bookmarks for the authenticated user.")
@PreAuthorize("isAuthenticated()")
public class BookmarkController {

    private final BookmarkService service;

    @GetMapping
    @Operation(summary = "List the authenticated user's bookmarks.")
    public ResponseEntity<Page<BookmarkResponse>> list(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listByUser(principal.getId(), pageable));
    }

    @PostMapping
    @Operation(summary = "Create a bookmark for the authenticated user.")
    public ResponseEntity<BookmarkResponse> create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestBody BookmarkCreateRequest request) {
        return ResponseEntity.ok(service.create(principal.getId(), request));
    }

    @DeleteMapping("/{bookmarkId}")
    @Operation(summary = "Delete one of the authenticated user's bookmarks.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID bookmarkId) {
        service.delete(principal.getId(), bookmarkId);
        return ResponseEntity.noContent().build();
    }
}
