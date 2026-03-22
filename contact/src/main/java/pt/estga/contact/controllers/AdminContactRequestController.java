package pt.estga.contact.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.contact.enums.ContactStatus;
import pt.estga.contact.entities.ContactRequest;
import pt.estga.contact.services.ContactRequestQueryService;
import pt.estga.contact.services.ContactRequestService;
import pt.estga.sharedweb.models.PagedRequest;

@RestController
@RequestMapping("/api/v1/admin/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Contact Requests", description = "Admin endpoints for contact requests.")
@PreAuthorize("hasRole('MODERATOR')")
public class AdminContactRequestController {

    private final ContactRequestQueryService queryService;
    private final ContactRequestService service;

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
