package ru.polyshelf.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.polyshelf.auth.config.JwtProperties;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.models.RefreshTokenEntity;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String ISSUER = "polyshelf-auth";
    private static final String AUDIENCE = "polyshelf-api";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokensResponse issue(UserEntity user) {
        String refreshToken = newRefreshToken();

        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setUserId(user.getId());
        stored.setTokenHash(hash(refreshToken));
        stored.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plus(properties.refreshTtl()));
        refreshTokenRepository.save(stored);

        return new AuthTokensResponse(accessToken(user), refreshToken, "Bearer", properties.accessTtl().toSeconds());
    }

    public Optional<UUID> consumeRefreshToken(String refreshToken) {
        String hash = hash(refreshToken);
        Optional<RefreshTokenEntity> stored = refreshTokenRepository.findByTokenHash(hash);
        if (stored.isEmpty() || refreshTokenRepository.deleteByTokenHash(hash) == 0) {
            return Optional.empty();
        }
        return stored
                .filter(token -> token.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
                .map(RefreshTokenEntity::getUserId);
    }

    private String accessToken(UserEntity user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTtl()))
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
