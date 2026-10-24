package ru.polyshelf.auth.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.auth.dto.AuthCredentials;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.UserRepository;
import ru.polyshelf.auth.dto.YandexAuthRequest;
import ru.polyshelf.auth.exception.AuthApiException;
import ru.polyshelf.auth.exception.AuthErrorCode;
import ru.polyshelf.auth.security.AuthUserDetails;
import ru.polyshelf.auth.yandex.YandexClient;
import ru.polyshelf.auth.yandex.YandexProfile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final AuthTokensResponse TOKENS = new AuthTokensResponse("access", "refresh", "Bearer", 900);

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private YandexClient yandexClient;

    @Mock
    private JwtDecoder jwtDecoder;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, tokenService, yandexClient, jwtDecoder);
        user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
    }

    @Test
    void registerStoresHashedPasswordAndNormalizedEmail() {
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue(any())).thenReturn(TOKENS);

        AuthTokensResponse response = authService.register(new AuthCredentials("  User@Example.COM ", "secret1"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals("user@example.com", captor.getValue().getEmail());
        assertNotEquals("secret1", captor.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches("secret1", captor.getValue().getPasswordHash()));
        assertEquals(TOKENS, response);
    }

    @Test
    void registerMapsDuplicateEmailToConflict() {
        when(userRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        AuthCredentials credentials = new AuthCredentials("user@example.com", "secret1");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(credentials));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void loginIssuesTokensForAuthenticatedUser() {
        AuthUserDetails details = new AuthUserDetails(user);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
        when(tokenService.issue(user)).thenReturn(TOKENS);

        AuthTokensResponse response = authService.login(new AuthCredentials(" User@example.com", "secret1"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("user@example.com", captor.getValue().getName());
        assertEquals(TOKENS, response);
    }

    @Test
    void loginMapsBadCredentialsToUnauthorized() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        AuthCredentials credentials = new AuthCredentials("user@example.com", "wrong-password");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(credentials));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    void refreshIssuesNewTokensForExistingUser() {
        when(tokenService.consumeRefreshToken("old")).thenReturn(Optional.of(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(TOKENS);

        assertEquals(TOKENS, authService.refresh("old"));
    }

    @Test
    void refreshRejectsInvalidToken() {
        when(tokenService.consumeRefreshToken("bad")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.refresh("bad"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refreshRejectsTokenOfDeletedUser() {
        when(tokenService.consumeRefreshToken("old")).thenReturn(Optional.of(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.refresh("old"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    private static final YandexProfile PROFILE = new YandexProfile("ya-1", "Ivan", null);

    @Test
    void registerRejectsEmailBoundToYandex() {
        UserEntity yandexUser = new UserEntity();
        yandexUser.setEmail("ivan@yandex.ru");
        yandexUser.setYandexId("ya-1");
        when(userRepository.findByEmail("ivan@yandex.ru")).thenReturn(Optional.of(yandexUser));

        AuthCredentials credentials = new AuthCredentials("ivan@yandex.ru", "secret1");
        AuthApiException ex = assertThrows(AuthApiException.class, () -> authService.register(credentials));

        assertEquals(AuthErrorCode.EMAIL_BOUND_TO_YANDEX, ex.getCode());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginExplainsThatEmailIsBoundToYandex() {
        UserEntity yandexUser = new UserEntity();
        yandexUser.setEmail("ivan@yandex.ru");
        yandexUser.setYandexId("ya-1");
        when(userRepository.findByEmail("ivan@yandex.ru")).thenReturn(Optional.of(yandexUser));

        AuthCredentials credentials = new AuthCredentials("ivan@yandex.ru", "secret1");
        AuthApiException ex = assertThrows(AuthApiException.class, () -> authService.login(credentials));

        assertEquals(AuthErrorCode.EMAIL_BOUND_TO_YANDEX, ex.getCode());
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void yandexCreatesNewUserWithoutPassword() {
        when(yandexClient.fetchProfile("tok")).thenReturn(PROFILE);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue(any())).thenReturn(TOKENS);

        assertEquals(TOKENS, authService.yandex(new YandexAuthRequest("tok", null)));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals("ivan@yandex.ru", captor.getValue().getEmail());
        assertEquals("ya-1", captor.getValue().getYandexId());
        assertNull(captor.getValue().getPasswordHash());
    }

    @Test
    void yandexLogsInAlreadyLinkedUser() {
        user.setYandexId("ya-1");
        when(yandexClient.fetchProfile("tok")).thenReturn(PROFILE);
        when(userRepository.findByYandexId("ya-1")).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(TOKENS);

        assertEquals(TOKENS, authService.yandex(new YandexAuthRequest("tok", null)));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void yandexAsksToConfirmLinkWhenEmailMatchesPasswordAccount() {
        user.setEmail("ivan@yandex.ru");
        when(yandexClient.fetchProfile("tok")).thenReturn(PROFILE);
        when(userRepository.findByEmail("ivan@yandex.ru")).thenReturn(Optional.of(user));

        AuthApiException ex = assertThrows(AuthApiException.class,
                () -> authService.yandex(new YandexAuthRequest("tok", null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(AuthErrorCode.YANDEX_LINK_REQUIRED, ex.getCode());
        assertNull(user.getYandexId());
    }

    @Test
    void yandexLinksAfterPasswordConfirmation() {
        user.setEmail("ivan@yandex.ru");
        user.setPasswordHash(passwordEncoder.encode("secret1"));
        when(yandexClient.fetchProfile("tok")).thenReturn(PROFILE);
        when(userRepository.findByEmail("ivan@yandex.ru")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(tokenService.issue(user)).thenReturn(TOKENS);

        assertEquals(TOKENS, authService.yandex(new YandexAuthRequest("tok", "secret1")));
        assertEquals("ya-1", user.getYandexId());
    }

    @Test
    void yandexRejectsLinkWithWrongPassword() {
        user.setEmail("ivan@yandex.ru");
        user.setPasswordHash(passwordEncoder.encode("secret1"));
        when(yandexClient.fetchProfile("tok")).thenReturn(PROFILE);
        when(userRepository.findByEmail("ivan@yandex.ru")).thenReturn(Optional.of(user));

        AuthApiException ex = assertThrows(AuthApiException.class,
                () -> authService.yandex(new YandexAuthRequest("tok", "wrong-pass")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertNull(user.getYandexId());
    }

    @Test
    void yandexEmailIsDerivedFromLogin() {
        assertEquals("petr.s@yandex.ru", new YandexProfile("ya-2", "Petr.S", null).email());
    }

    @Test
    void changePasswordSetsPasswordForYandexOnlyUser() {
        user.setPasswordHash(null);
        user.setYandexId("ya-1");
        authorizeAsUser();

        authService.changePassword("Bearer token", null, "secret1");

        assertTrue(passwordEncoder.matches("secret1", user.getPasswordHash()));
        assertEquals("ya-1", user.getYandexId());
    }

    @Test
    void changePasswordReplacesPasswordWhenCurrentMatches() {
        user.setPasswordHash(passwordEncoder.encode("old-secret"));
        authorizeAsUser();

        authService.changePassword("Bearer token", "old-secret", "new-secret");

        assertTrue(passwordEncoder.matches("new-secret", user.getPasswordHash()));
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        String oldHash = passwordEncoder.encode("old-secret");
        user.setPasswordHash(oldHash);
        authorizeAsUser();

        AuthApiException ex = assertThrows(AuthApiException.class,
                () -> authService.changePassword("Bearer token", "wrong", "new-secret"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(AuthErrorCode.INVALID_PASSWORD, ex.getCode());
        assertEquals(oldHash, user.getPasswordHash());
    }

    @Test
    void changePasswordRequiresCurrentPasswordWhenOneIsSet() {
        authorizeAsUser();

        AuthApiException ex = assertThrows(AuthApiException.class,
                () -> authService.changePassword("Bearer token", null, "new-secret"));

        assertEquals(AuthErrorCode.INVALID_PASSWORD, ex.getCode());
        assertEquals("hash", user.getPasswordHash());
    }

    @Test
    void changePasswordRequiresBearerToken() {
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> authService.changePassword(null, null, "secret1"));
        ResponseStatusException wrongScheme = assertThrows(ResponseStatusException.class,
                () -> authService.changePassword("Basic abc", null, "secret1"));

        assertEquals(HttpStatus.UNAUTHORIZED, missing.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, wrongScheme.getStatusCode());
        verifyNoInteractions(jwtDecoder, userRepository);
    }

    @Test
    void changePasswordRejectsInvalidToken() {
        when(jwtDecoder.decode("bad")).thenThrow(new BadJwtException("bad"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.changePassword("Bearer bad", null, "secret1"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void changePasswordRejectsUnknownUser() {
        when(jwtDecoder.decode("token")).thenReturn(jwtFor(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.changePassword("Bearer token", null, "secret1"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    private void authorizeAsUser() {
        when(jwtDecoder.decode("token")).thenReturn(jwtFor(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private static Jwt jwtFor(UUID id) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(id.toString()).build();
    }

}
