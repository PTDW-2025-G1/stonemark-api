package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.services.UserService;

@RestController
@RequestMapping("/api/v1/public/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Public endpoints for users.")
public class PublicUserController {

    private final UserService service;
    private final UserMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserPublicDto> publicGetById(
            @Parameter(description = "ID of the user to be retrieved", required = true)
            @PathVariable Long id) {
        return service.findById(id)
                .map(mapper::toPublicDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
