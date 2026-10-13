package ru.polyshelf.gateway.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthTokensResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {

    private static final String BEARER = "Bearer";

    public static AuthTokensResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new AuthTokensResponse(accessToken, refreshToken, BEARER, expiresIn);
    }
}
