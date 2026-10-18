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
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.auth.dto.AuthCredentials;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.UserRepository;
import ru.polyshelf.auth.security.AuthUserDetails;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, tokenService);
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

}
