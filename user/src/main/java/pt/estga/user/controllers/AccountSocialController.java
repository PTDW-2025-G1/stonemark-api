package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.dtos.LinkedProviderDto;
import pt.estga.user.entities.User;
import pt.estga.user.services.AccountSocialService;
import pt.estga.user.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account/socials")
@RequiredArgsConstructor
@Tag(name = "Social Accounts", description = "Management of social accounts linked to the user.")
@PreAuthorize("isAuthenticated()")
public class AccountSocialController {

    private final AccountSocialService service;
    private final UserService userService;

    @GetMapping("/providers")
    @Operation(
            summary = "Get linked social providers",
            description = "Returns the list of social providers linked to the authenticated user."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Linked providers retrieved successfully"
    )
    public ResponseEntity<List<LinkedProviderDto>> getLinkedProviders(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User connectedUser = userService.findById(principal.getId()).orElseThrow();
        List<LinkedProviderDto> providers =
                service.getLinkedProviders(connectedUser);

        return ResponseEntity.ok(providers);
    }
}
