package com.ondeehoje.auth.dto;

public record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {
    @Override public String toString() { return "AccessTokenResponse[REDACTED]"; }
}
