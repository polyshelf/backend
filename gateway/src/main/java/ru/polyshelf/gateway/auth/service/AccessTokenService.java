package ru.polyshelf.gateway.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.polyshelf.gateway.security.AuthenticatedUser;
import ru.polyshelf.gateway.security.JwtProperties;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public AccessToken issue(int userId, String email) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(Integer.toString(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(AuthenticatedUser.EMAIL_CLAIM, email)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
