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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.shared.dtos.MessageResponseDto;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.user.dtos.*;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.services.AccountService;
import pt.estga.user.services.PasswordService;
import pt.estga.user.services.UserService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Self-service operations for logged-in users.")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final UserService userService;
    private final AccountService accountService;
    private final UserMapper mapper;
    private final PasswordService passwordService;
    private final MediaService mediaService;

    @Operation(summary = "Get user profile", description = "Retrieves the profile information of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)))
    })
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfileInfo(@AuthenticationPrincipal AppPrincipal principal) {
        User user = userService
                .findByIdWithContacts(principal.getId())
                .orElseThrow();
        return ResponseEntity.ok(mapper.toDto(user));
    }

    @GetMapping("/security/status")
    @Operation(
            summary = "Get account security status",
            description = "Returns information about available authentication methods."
    )
    public ResponseEntity<AccountSecurityStatusDto> getSecurityStatus(@AuthenticationPrincipal AppPrincipal principal) {
        User user = userService.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(accountService.getSecurityStatus(user));
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
            @AuthenticationPrincipal AppPrincipal principal,
            @Parameter(description = "Updated profile information", required = true)
            @Valid @RequestBody ProfileUpdateRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        mapper.update(user, request);

        if (request.photoId() != null) {
            mediaService.findById(request.photoId()).ifPresent(user::setPhoto);
        }

        userService.update(user);
        return ResponseEntity.ok(new MessageResponseDto("Your profile has been updated successfully."));
    }

    @Operation(summary = "Change user password", description = "Changes the password for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid password change request")
    })
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponseDto> changePassword(
            @AuthenticationPrincipal AppPrincipal principal,
            @Parameter(description = "Password change request details", required = true)
            @Valid @RequestBody PasswordChangeRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        passwordService.changePassword(user, request);
        return ResponseEntity.ok(new MessageResponseDto("Your password has been changed successfully."));
    }

    @Operation(summary = "Set user password", description = "Sets a new password for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password set successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid password set request")
    })
    @PostMapping("/set-password")
    public ResponseEntity<MessageResponseDto> setPassword(
            @AuthenticationPrincipal AppPrincipal principal,
            @Parameter(description = "Password set request details", required = true)
            @Valid @RequestBody PasswordSetRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        passwordService.setPassword(user, request);
        return ResponseEntity.ok(new MessageResponseDto("Your password has been set successfully."));
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> uploadPhoto(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        User user = userService.findById(principal.getId()).orElseThrow();
        MediaFile mediaFile = mediaService.save(file.getInputStream(), file.getOriginalFilename());
        user.setPhoto(mediaFile);
        User updatedUser = userService.update(user);
        return ResponseEntity.ok(mapper.toDto(updatedUser));
    }

    @Operation(summary = "Delete user account", description = "Deletes the authenticated user's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deleted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping
    public ResponseEntity<MessageResponseDto> deleteAccount(@AuthenticationPrincipal AppPrincipal principal) {
        userService.softDeleteUser(principal.getId());
        return ResponseEntity.ok(new MessageResponseDto("Your account has been deleted successfully."));
    }
}
