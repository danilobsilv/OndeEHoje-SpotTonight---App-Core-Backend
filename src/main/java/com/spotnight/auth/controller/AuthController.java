package com.spotnight.auth.controller;

import com.spotnight.auth.dto.*;
import com.spotnight.auth.security.AuthProperties;
import com.spotnight.auth.service.AuthService;
import com.spotnight.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public static final String REFRESH_COOKIE = "refresh_token";
    private final AuthService auth;
    private final AuthProperties properties;
    public AuthController(AuthService auth, AuthProperties properties) {
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return session(auth.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String raw) {
        return session(auth.refresh(raw));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String raw) {
        auth.logout(raw);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private ResponseEntity<AccessTokenResponse> session(AuthService.SessionTokens tokens) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken(), properties.refreshTokenTtl()))
                .body(tokens.accessToken());
    }
    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(properties.cookieSecure())
                .sameSite("Strict").path("/api/v1/auth").maxAge(maxAge).build().toString();
    }
    public record CsrfResponse(String headerName, String token) {}
}
