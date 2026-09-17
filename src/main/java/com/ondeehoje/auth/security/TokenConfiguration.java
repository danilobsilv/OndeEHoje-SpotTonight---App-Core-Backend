package com.ondeehoje.auth.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class TokenConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties properties, Environment environment) {
        if (!properties.cookieSecure() && (!environment.acceptsProfiles(Profiles.of("local"))
                || environment.acceptsProfiles(Profiles.of("prod")))) {
            throw new IllegalStateException("Insecure cookies are allowed only in the local profile without prod.");
        }
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(key(properties)).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                var roles = jwt.getClaimAsStringList("roles");
                if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null
                        || !jwt.getExpiresAt().isAfter(clock.instant())
                        || jwt.getIssuedAt().isAfter(clock.instant())
                        || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                        || !jwt.getAudience().contains(properties.audience())
                        || roles == null || roles.isEmpty()
                        || roles.stream().anyMatch(role -> !Set.of("USER", "ADMIN").contains(role))) {
                    return invalid();
                }
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ex) { return invalid(); }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps,
                new JwtIssuerValidator(properties.issuer()), requiredClaims));
        return decoder;
    }

    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token.", null));
    }
    private static SecretKeySpec key(AuthProperties properties) {
        return new SecretKeySpec(Base64.getDecoder().decode(properties.jwtSecret()), "HmacSHA256");
    }
}
