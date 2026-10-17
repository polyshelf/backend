package ru.polyshelf.core.security;

import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        UserRole role
) {
}
