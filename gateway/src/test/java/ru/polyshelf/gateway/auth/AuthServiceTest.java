package ru.polyshelf.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.polyshelf.gateway.auth.client.IdentityClient;
import ru.polyshelf.gateway.auth.client.IdentitySession;
import ru.polyshelf.gateway.auth.service.AccessTokenService;
import ru.polyshelf.gateway.auth.service.AuthService;
import ru.polyshelf.gateway.auth.web.AuthCredentials;
import ru.polyshelf.gateway.auth.web.AuthTokensResponse;
import ru.polyshelf.gateway.auth.web.RefreshTokenRequest;
import ru.polyshelf.gateway.common.config.ClockConfiguration;
import ru.polyshelf.gateway.security.AuthenticatedUser;
import ru.polyshelf.gateway.security.JwtConfiguration;
import ru.polyshelf.gateway.security.JwtProperties;
import ru.polyshelf.gateway.security.RsaKeyLoader;
import ru.polyshelf.gateway.security.RsaKeyPair;

class AuthServiceTest {

    private static final Instant REFRESH_EXPIRES = Instant.parse("2030-01-01T00:00:00Z");

    private final IdentityClient identity = mock(IdentityClient.class);
    private final JwtProperties properties =
            new JwtProperties("polyshelf-gateway", Duration.ofMinutes(15), "", "", true);
    private final RsaKeyPair keys = new RsaKeyLoader(new DefaultResourceLoader()).load("", "", true);
    private final JwtConfiguration jwt = new JwtConfiguration();
    private final AuthService service = new AuthService(identity,
            new AccessTokenService(jwt.jwtEncoder(keys), properties, new ClockConfiguration().clock()));

    @Test
    void registerNormalizesEmailAndIssuesVerifiableToken() {
        when(identity.register("user@example.com", "secret1"))
                .thenReturn(new IdentitySession(42, "user@example.com", "refresh-1", REFRESH_EXPIRES));

        AuthTokensResponse response = service.register(new AuthCredentials("  User@Example.COM ", "secret1"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.refreshToken()).isEqualTo("refresh-1");
        assertThat(response.expiresIn()).isEqualTo(900);
        Jwt decoded = jwt.jwtDecoder(keys, properties).decode(response.accessToken());
        AuthenticatedUser user = AuthenticatedUser.from(decoded);
        assertThat(user.id()).isEqualTo(42);
        assertThat(user.email()).isEqualTo("user@example.com");
    }

    @Test
    void loginAndRefreshDelegateToIdentity() {
        IdentitySession session = new IdentitySession(7, "a@b.co", "r2", REFRESH_EXPIRES);
        when(identity.login("a@b.co", "secret")).thenReturn(session);
        when(identity.refresh("r1")).thenReturn(session);

        assertThat(service.login(new AuthCredentials("A@B.co", "secret")).refreshToken()).isEqualTo("r2");
        assertThat(service.refresh(new RefreshTokenRequest("r1")).refreshToken()).isEqualTo("r2");
    }
}
