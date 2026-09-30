package com.spotnight.auth.service;

import com.spotnight.auth.dto.AccessTokenResponse;
import com.spotnight.auth.security.AuthProperties;
import com.spotnight.user.model.User;
import java.time.Clock;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;
    public AccessTokenService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }
    public AccessTokenResponse issue(User user) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).subject(user.getId().toString())
                .audience(List.of(properties.audience())).issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new AccessTokenResponse(token, "Bearer", properties.accessTokenTtl().toSeconds());
    }
}
