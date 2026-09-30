package com.ondeehoje.establishment.controller;

import com.ondeehoje.establishment.dto.EstablishmentCreateRequest;
import com.ondeehoje.establishment.dto.EstablishmentResponse;
import com.ondeehoje.establishment.service.EstablishmentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/establishments")
public class EstablishmentController {
    private final EstablishmentService establishments;

    public EstablishmentController(EstablishmentService establishments) {
        this.establishments = establishments;
    }

    @PostMapping
    public ResponseEntity<EstablishmentResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EstablishmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(establishments.create(UUID.fromString(jwt.getSubject()), request));
    }
}
