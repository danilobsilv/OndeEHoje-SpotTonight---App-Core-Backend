package com.ondeehoje.auth.security;

import static org.assertj.core.api.Assertions.*;
import com.ondeehoje.auth.service.AccessTokenService;
import com.ondeehoje.user.model.User;
import java.time.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenConfigurationTest {
    private final TokenConfiguration configuration = new TokenConfiguration();
    private final Instant now = Instant.parse("2026-09-17T12:00:00Z");
    private AuthProperties properties(boolean secure) {
        return new AuthProperties(Base64.getEncoder().encodeToString(new byte[32]), "issuer", "audience",
                Duration.ofMinutes(15), Duration.ofDays(30), secure);
    }

    @Test
    void jwtUsesMinimalClaimsAndRejectsExpiredAndWrongSignatureTokens() {
        var properties = properties(true);
        var service = new AccessTokenService(configuration.jwtEncoder(properties, new MockEnvironment()),
                properties, Clock.fixed(now, ZoneOffset.UTC));
        var user = new User("Alice", "alice@example.com", "hash", now);
        var token = service.issue(user);
        var jwt = configuration.jwtDecoder(properties, Clock.fixed(now, ZoneOffset.UTC)).decode(token.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaims().keySet()).containsExactlyInAnyOrder("iss", "sub", "aud", "iat", "exp", "roles");
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(900));
        assertThatThrownBy(() -> configuration.jwtDecoder(properties,
                Clock.fixed(now.plusSeconds(900), ZoneOffset.UTC)).decode(token.accessToken())).isInstanceOf(JwtException.class);
        byte[] other = new byte[32];
        other[0] = 1;
        var wrongKey = new AuthProperties(Base64.getEncoder().encodeToString(other), "issuer", "audience",
                Duration.ofMinutes(15), Duration.ofDays(30), true);
        assertThatThrownBy(() -> configuration.jwtDecoder(wrongKey, Clock.fixed(now, ZoneOffset.UTC))
                .decode(token.accessToken())).isInstanceOf(JwtException.class);
    }

    @Test
    void configurationRejectsWeakKeysLongAccessTtlAndInsecureProductionCookies() {
        assertThatThrownBy(() -> new AuthProperties("c2hvcnQ=", "issuer", "audience",
                Duration.ofMinutes(15), Duration.ofDays(30), true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthProperties(properties(true).jwtSecret(), "issuer", "audience",
                Duration.ofDays(1), Duration.ofDays(30), true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> configuration.jwtEncoder(properties(false),
                new MockEnvironment().withProperty("spring.profiles.active", "local,prod")))
                .isInstanceOf(IllegalStateException.class);
        var local = new MockEnvironment();
        local.setActiveProfiles("local");
        assertThat(configuration.jwtEncoder(properties(false), local)).isNotNull();
    }
}
