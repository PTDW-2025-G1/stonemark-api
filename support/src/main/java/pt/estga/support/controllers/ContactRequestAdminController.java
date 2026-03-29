package pt.estga.support.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.support.enums.ContactStatus;
import pt.estga.support.entities.ContactRequest;
import pt.estga.support.services.ContactRequestQueryService;
import pt.estga.support.services.ContactRequestCommandService;
import pt.estga.sharedweb.models.PagedRequest;

@RestController
@RequestMapping("/api/v1/admin/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Contact Requests", description = "Admin endpoints for contact requests.")
@PreAuthorize("hasRole('MODERATOR')")
public class ContactRequestAdminController {

    private final ContactRequestQueryService queryService;
    private final ContactRequestCommandService service;

    @PostMapping("/search")
    public ResponseEntity<Page<ContactRequest>> search(@RequestBody PagedRequest request) {
        return ResponseEntity.ok(queryService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactRequest> getById(@PathVariable Long id) {
        return queryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ContactRequest> updateStatus(
            @PathVariable Long id,
            @RequestParam ContactStatus status
    ) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
