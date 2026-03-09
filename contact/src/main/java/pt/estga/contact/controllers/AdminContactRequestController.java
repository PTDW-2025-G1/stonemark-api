package pt.estga.contact.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.contact.ContactStatus;
import pt.estga.contact.entities.ContactRequest;
import pt.estga.contact.services.ContactRequestService;

@RestController
@RequestMapping("/api/v1/admin/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Contact Requests", description = "Admin endpoints for contact requests.")
@PreAuthorize("hasRole('MODERATOR')")
public class AdminContactRequestController {

    private final ContactRequestService service;

    @GetMapping
    public ResponseEntity<Page<ContactRequest>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactRequest> getById(@PathVariable Long id) {
        return service.findById(id)
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
