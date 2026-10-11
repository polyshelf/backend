package ru.polyshelf.gateway.auth.client;

import java.time.Instant;

public record IdentitySession(int userId, String email, String refreshToken, Instant refreshTokenExpiresAt) {
}
