package ru.polyshelf.auth.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import ru.polyshelf.auth.TestKeys;
import ru.polyshelf.auth.config.JwtConfig;
import ru.polyshelf.auth.config.JwtProperties;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.models.RefreshTokenEntity;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.RefreshTokenRepository;
import ru.polyshelf.auth.security.UserRole;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private TokenService tokenService;
    private NimbusJwtDecoder decoder;
    private UserEntity user;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair keyPair = TestKeys.generate();
        JwtProperties properties = new JwtProperties(
                new FileSystemResource(TestKeys.writePrivateKey(keyPair)),
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );
        tokenService = new TokenService(new JwtConfig().jwtEncoder(properties), properties, refreshTokenRepository);
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();

        user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.ADMIN);
    }

    private RefreshTokenEntity savedToken() {
        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void accessTokenIsSignedForApiWithRole() {
        AuthTokensResponse tokens = tokenService.issue(user);

        Jwt jwt = decoder.decode(tokens.accessToken());

        assertEquals("RS256", jwt.getHeaders().get("alg"));
        assertEquals(USER_ID.toString(), jwt.getSubject());
        assertEquals("polyshelf-auth", jwt.getIssuer().toString());
        assertEquals(List.of("polyshelf-api"), jwt.getAudience());
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertEquals("Bearer", tokens.tokenType());
        assertEquals(900, tokens.expiresIn());
    }

    @Test
    void storesOnlyHashOfRefreshToken() {
        AuthTokensResponse tokens = tokenService.issue(user);

        RefreshTokenEntity saved = savedToken();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(tokens.refreshToken(), saved.getTokenHash());
        assertTrue(saved.getExpiresAt().isAfter(OffsetDateTime.now().plusDays(29)));
    }

    @Test
    void consumesValidRefreshTokenOnce() {
        AuthTokensResponse tokens = tokenService.issue(user);
        RefreshTokenEntity saved = savedToken();
        when(refreshTokenRepository.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));
        when(refreshTokenRepository.deleteByTokenHash(saved.getTokenHash())).thenReturn(1);

        assertEquals(Optional.of(USER_ID), tokenService.consumeRefreshToken(tokens.refreshToken()));
    }

    @Test
    void rejectsUnknownRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), tokenService.consumeRefreshToken("unknown"));
        verify(refreshTokenRepository, never()).deleteByTokenHash(anyString());
    }

    @Test
    void rejectsExpiredRefreshToken() {
        AuthTokensResponse tokens = tokenService.issue(user);
        RefreshTokenEntity saved = savedToken();
        saved.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));
        when(refreshTokenRepository.deleteByTokenHash(saved.getTokenHash())).thenReturn(1);

        assertEquals(Optional.empty(), tokenService.consumeRefreshToken(tokens.refreshToken()));
    }

    @Test
    void rejectsRefreshTokenConsumedConcurrently() {
        AuthTokensResponse tokens = tokenService.issue(user);
        RefreshTokenEntity saved = savedToken();
        when(refreshTokenRepository.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));
        when(refreshTokenRepository.deleteByTokenHash(saved.getTokenHash())).thenReturn(0);

        assertEquals(Optional.empty(), tokenService.consumeRefreshToken(tokens.refreshToken()));
    }

}
