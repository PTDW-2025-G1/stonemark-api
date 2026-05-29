package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.sharedweb.dtos.MessageResponseDto;
import pt.estga.user.dtos.*;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.services.UserService;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Self-service operations for logged-in users. Password management via Keycloak.")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final UserService userService;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Get user profile", description = "Retrieves the profile information of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)))
    })
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfileInfo(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService
                .findByIdForProfile(principal.getId())
                .orElseThrow();
        return ResponseEntity.ok(mapper.toDto(user));
    }

    @Operation(summary = "Update user profile", description = "Updates the profile information of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid profile data provided")
    })
    @PutMapping("/profile")
    public ResponseEntity<MessageResponseDto> updateProfile(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Parameter(description = "Updated profile information", required = true)
            @Valid @RequestBody ProfileUpdateRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        mapper.update(user, request);
        userService.update(user);
        return ResponseEntity.ok(MessageResponseDto.success("Your profile has been updated successfully."));
    }

    @Operation(summary = "Delete user account", description = "Deletes the authenticated user's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deleted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping
    public ResponseEntity<MessageResponseDto> deleteAccount(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        userService.softDeleteUser(principal.getId());
        return ResponseEntity.ok(MessageResponseDto.success("Your account has been deleted successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<MeDto> me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService.findByIdForProfile(principal.getId()).orElseThrow();
        return ResponseEntity.ok(new MeDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getPasswordHash() != null,
                user.isEnabled(),
                user.isAccountLocked()
        ));
    }

    @PostMapping("/password")
    public ResponseEntity<MessageResponseDto> setPassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody PasswordSetRequest request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        if (user.getPasswordHash() != null) {
            return ResponseEntity.badRequest()
                    .body(MessageResponseDto.error("Password is already set. Use PUT to change it."));
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userService.update(user);
        return ResponseEntity.ok(MessageResponseDto.success("Password set successfully."));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponseDto> changePassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody PasswordChangeRequest request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        if (user.getPasswordHash() == null) {
            return ResponseEntity.badRequest()
                    .body(MessageResponseDto.error("No password set. Use POST to set one first."));
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(MessageResponseDto.error("Current password is incorrect."));
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userService.update(user);
        return ResponseEntity.ok(MessageResponseDto.success("Password changed successfully."));
    }
}
