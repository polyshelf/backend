package ru.polyshelf.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserAuthenticationConverterTest {

    private final UserAuthenticationConverter converter = new UserAuthenticationConverter();

    @Test
    void convert_shouldMapUserIdAndUserRole() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(userId, "USER");

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, authentication);
        assertEquals(new UserPrincipal(userId, UserRole.USER), authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_USER")));
    }

    @Test
    void convert_shouldMapAdminRole() {
        UUID userId = UUID.randomUUID();
        var authentication = converter.convert(jwt(userId, "ADMIN"));

        assertNotNull(authentication);
        assertEquals(new UserPrincipal(userId, UserRole.ADMIN), authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_ADMIN")));
    }

    private static Jwt jwt(UUID userId, String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }

}
