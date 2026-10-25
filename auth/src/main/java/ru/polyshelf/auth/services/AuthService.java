package ru.polyshelf.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.auth.dto.AuthCredentials;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.dto.YandexAuthRequest;
import ru.polyshelf.auth.exception.AuthApiException;
import ru.polyshelf.auth.exception.AuthErrorCode;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.UserRepository;
import ru.polyshelf.auth.security.AuthUserDetails;
import ru.polyshelf.auth.yandex.YandexClient;
import ru.polyshelf.auth.yandex.YandexProfile;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final YandexClient yandexClient;
    private final JwtDecoder jwtDecoder;

    @Transactional
    public AuthTokensResponse register(AuthCredentials credentials) {
        String email = normalize(credentials.email());
        userRepository.findByEmail(email).ifPresent(existing -> {
            throw emailConflict(existing);
        });

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(credentials.password()));

        try {
            return tokenService.issue(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException _) {
            throw new AuthApiException(HttpStatus.CONFLICT, AuthErrorCode.EMAIL_TAKEN, "Email уже зарегистрирован");
        }
    }

    public AuthTokensResponse login(AuthCredentials credentials) {
        userRepository.findByEmail(normalize(credentials.email()))
                .filter(existing -> existing.getPasswordHash() == null)
                .ifPresent(existing -> {
                    throw emailConflict(existing);
                });
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
    public AuthTokensResponse yandex(YandexAuthRequest request) {
        YandexProfile profile = yandexClient.fetchProfile(request.accessToken());

        var linked = userRepository.findByYandexId(profile.id());
        if (linked.isPresent()) {
            return tokenService.issue(linked.get());
        }

        String email = profile.email();
        var sameEmail = profile.emails().stream()
                .map(userRepository::findByEmail)
                .flatMap(Optional::stream)
                .findFirst();
        if (sameEmail.isPresent()) {
            UserEntity existing = sameEmail.get();
            if (existing.getYandexId() != null || existing.getPasswordHash() == null) {
                throw new AuthApiException(HttpStatus.CONFLICT, AuthErrorCode.EMAIL_TAKEN,
                        "Этот email уже привязан к другому Яндекс-аккаунту");
            }
            return linkYandex(existing, profile, request.linkPassword());
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setYandexId(profile.id());
        try {
            return tokenService.issue(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException _) {
            throw new AuthApiException(HttpStatus.SERVICE_UNAVAILABLE, AuthErrorCode.TRY_AGAIN,
                    "Не удалось создать аккаунт, повторите попытку");
        }
    }

    private AuthTokensResponse linkYandex(UserEntity existing, YandexProfile profile, String linkPassword) {
        if (linkPassword == null || linkPassword.isBlank()) {
            throw new AuthApiException(HttpStatus.CONFLICT, AuthErrorCode.YANDEX_LINK_REQUIRED,
                    "Аккаунт с email " + existing.getEmail() + " уже есть. Подтвердите привязку Яндекса паролем");
        }
        if (!passwordEncoder.matches(linkPassword, existing.getPasswordHash())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_PASSWORD, "Неверный пароль");
        }
        existing.setYandexId(profile.id());
        return tokenService.issue(userRepository.saveAndFlush(existing));
    }

    @Transactional
    public void changePassword(String authorization, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId(authorization))
                .orElseThrow(() -> unauthorized("Требуется авторизация"));
        if (user.getPasswordHash() != null
                && (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash()))) {
            throw new AuthApiException(HttpStatus.FORBIDDEN, AuthErrorCode.INVALID_PASSWORD, "Неверный текущий пароль");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private UUID userId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw unauthorized("Требуется авторизация");
        }
        try {
            return UUID.fromString(jwtDecoder.decode(authorization.substring("Bearer ".length())).getSubject());
        } catch (JwtException | IllegalArgumentException _) {
            throw unauthorized("Требуется авторизация");
        }
    }

    private static AuthApiException emailConflict(UserEntity existing) {
        if (existing.getPasswordHash() == null) {
            return new AuthApiException(HttpStatus.CONFLICT, AuthErrorCode.EMAIL_BOUND_TO_YANDEX,
                    "Этот email привязан к Яндексу, войдите через Яндекс");
        }
        return new AuthApiException(HttpStatus.CONFLICT, AuthErrorCode.EMAIL_TAKEN, "Email уже зарегистрирован");
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
