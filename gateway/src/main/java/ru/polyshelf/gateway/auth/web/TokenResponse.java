package ru.polyshelf.gateway.auth.web;

import java.time.Instant;

public record TokenResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse bearer(String accessToken,
                                       Instant accessTokenExpiresAt,
                                       String refreshToken,
                                       Instant refreshTokenExpiresAt) {
        return new TokenResponse(BEARER, accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt);
    }
}
