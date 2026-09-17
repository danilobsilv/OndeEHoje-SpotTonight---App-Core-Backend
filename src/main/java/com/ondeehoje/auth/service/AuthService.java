package com.ondeehoje.auth.service;

import com.ondeehoje.auth.dto.*;
import com.ondeehoje.auth.model.RefreshToken;
import com.ondeehoje.auth.repository.RefreshTokenRepository;
import com.ondeehoje.auth.security.AuthProperties;
import com.ondeehoje.shared.error.ApiException;
import com.ondeehoje.user.dto.UserResponse;
import com.ondeehoje.user.model.*;
import com.ondeehoje.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final RefreshTokenCodec codec;
    private final AccessTokenService accessTokens;
    private final AuthProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwords,
            RefreshTokenCodec codec, AccessTokenService accessTokens, AuthProperties properties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.codec = codec;
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwords.encode(codec.generate());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "VALIDATION_ERROR", "Password must not exceed 72 UTF-8 bytes.");
        if (users.existsByEmail(request.email())) throw duplicateEmail();
        var user = new User(request.name(), request.email(), passwords.encode(request.password()), clock.instant());
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException ex) {
            if (EmailUniqueConstraint.isViolation(ex)) throw duplicateEmail();
            throw ex;
        }
        return UserResponse.from(user);
    }

    @Transactional
    public SessionTokens login(LoginRequest request) {
        var user = users.findByEmail(request.email()).orElse(null);
        boolean matches = request.password().getBytes(StandardCharsets.UTF_8).length <= 72
                && passwords.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (!matches || user == null || user.getStatus() != UserStatus.ACTIVE)
            throw new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password.");
        return issue(user);
    }

    @Transactional
    public SessionTokens refresh(String raw) {
        if (!codec.validFormat(raw)) throw invalidRefresh();
        var token = refreshTokens.findForUpdate(codec.hash(raw)).orElseThrow(AuthService::invalidRefresh);
        if (!token.isUsable(clock.instant()) || token.getUser().getStatus() != UserStatus.ACTIVE)
            throw invalidRefresh();
        token.revoke(clock.instant());
        return issue(token.getUser());
    }

    @Transactional
    public void logout(String raw) {
        if (codec.validFormat(raw))
            refreshTokens.findForUpdate(codec.hash(raw)).ifPresent(token -> token.revoke(clock.instant()));
    }

    private SessionTokens issue(User user) {
        var now = clock.instant();
        String raw = codec.generate();
        refreshTokens.save(new RefreshToken(user, codec.hash(raw), now, now.plus(properties.refreshTokenTtl())));
        return new SessionTokens(accessTokens.issue(user), raw);
    }
    private static ApiException invalidRefresh() {
        return new ApiException(401, "INVALID_REFRESH_TOKEN", "Invalid or expired refresh token.");
    }
    private static ApiException duplicateEmail() {
        return new ApiException(409, "EMAIL_ALREADY_EXISTS", "Email is already registered.");
    }
    public record SessionTokens(AccessTokenResponse accessToken, String refreshToken) {
        @Override public String toString() { return "SessionTokens[REDACTED]"; }
    }
}
