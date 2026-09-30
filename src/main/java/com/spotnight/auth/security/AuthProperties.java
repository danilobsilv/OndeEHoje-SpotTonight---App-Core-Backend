package com.spotnight.auth.security;

import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(String jwtSecret, String issuer, String audience,
                             Duration accessTokenTtl, Duration refreshTokenTtl, boolean cookieSecure) {
    public AuthProperties {
        byte[] key;
        try { key = Base64.getDecoder().decode(jwtSecret == null ? "" : jwtSecret); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("JWT_SECRET must be Base64 encoded."); }
        if (key.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes.");
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank())
            throw new IllegalArgumentException("JWT issuer and audience are required.");
        if (accessTokenTtl == null || accessTokenTtl.compareTo(Duration.ofSeconds(1)) < 0
                || accessTokenTtl.compareTo(Duration.ofMinutes(30)) > 0)
            throw new IllegalArgumentException("Access token TTL must be between 1 second and 30 minutes.");
        if (refreshTokenTtl == null || refreshTokenTtl.compareTo(accessTokenTtl) <= 0
                || refreshTokenTtl.compareTo(Duration.ofDays(90)) > 0)
            throw new IllegalArgumentException("Refresh token TTL must exceed access TTL and be at most 90 days.");
    }
    @Override public String toString() { return "AuthProperties[REDACTED]"; }
}
