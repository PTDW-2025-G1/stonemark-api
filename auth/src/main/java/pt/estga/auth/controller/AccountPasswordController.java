package pt.estga.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.auth.dto.PasswordChangeRequest;
import pt.estga.auth.dto.PasswordSetRequest;
import pt.estga.auth.service.PasswordService;
import pt.estga.commoncore.interfaces.AuthenticatedPrincipal;
import pt.estga.commonweb.dtos.MessageResponseDto;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AccountPasswordController {

    private final PasswordService passwordService;

    @PostMapping("/password")
    public ResponseEntity<MessageResponseDto> setPassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody PasswordSetRequest request) {
        passwordService.setPassword(principal.getId(), request.password());
        return ResponseEntity.ok(MessageResponseDto.success("Password set successfully."));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponseDto> changePassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.changePassword(principal.getId(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(MessageResponseDto.success("Password changed successfully."));
    }
}
