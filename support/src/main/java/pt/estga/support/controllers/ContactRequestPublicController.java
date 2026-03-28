package pt.estga.support.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.entities.ContactRequest;
import pt.estga.support.services.ContactRequestService;

@RestController
@RequestMapping("/api/v1/public/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Public Contact Requests", description = "Public endpoints for contact requests.")
public class ContactRequestPublicController {

    private final ContactRequestService service;

    @PostMapping
    public ResponseEntity<ContactRequest> create(
            @Valid @RequestBody ContactRequestDto dto,
            @AuthenticationPrincipal AuthenticatedPrincipal principal
    ) {
        ContactRequest created = service.create(dto, principal.getId());
        return ResponseEntity.ok(created);
    }
}
