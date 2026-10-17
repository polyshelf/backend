package ru.polyshelf.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.auth.dto.AuthCredentials;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.UserRepository;
import ru.polyshelf.auth.security.AuthUserDetails;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Transactional
    public AuthTokensResponse register(AuthCredentials credentials) {
        UserEntity user = new UserEntity();
        user.setEmail(normalize(credentials.email()));
        user.setPasswordHash(passwordEncoder.encode(credentials.password()));

        try {
            return tokenService.issue(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
        }
    }

    public AuthTokensResponse login(AuthCredentials credentials) {
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalize(credentials.email()), credentials.password()));
            return tokenService.issue(((AuthUserDetails) Objects.requireNonNull(authentication.getPrincipal())).user());
        } catch (AuthenticationException _) {
            throw unauthorized("Неверный email или пароль");
        }
    }

    @Transactional
    public AuthTokensResponse refresh(String refreshToken) {
        return tokenService.consumeRefreshToken(refreshToken)
                .flatMap(userRepository::findById)
                .map(tokenService::issue)
                .orElseThrow(() -> unauthorized("Refresh token недействителен или истёк"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }

}
