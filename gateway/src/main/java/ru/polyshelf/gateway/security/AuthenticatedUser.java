package ru.polyshelf.gateway.security;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(int id, String email) {

    public static final String EMAIL_CLAIM = "email";

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(Integer.parseInt(jwt.getSubject()), jwt.getClaimAsString(EMAIL_CLAIM));
    }
}
