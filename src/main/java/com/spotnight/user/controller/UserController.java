package com.spotnight.user.controller;

import com.spotnight.user.dto.UserResponse;
import com.spotnight.user.service.UserService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }

    @GetMapping("/me")
    public UserResponse current(@AuthenticationPrincipal Jwt jwt) {
        return users.current(UUID.fromString(jwt.getSubject()));
    }
}
