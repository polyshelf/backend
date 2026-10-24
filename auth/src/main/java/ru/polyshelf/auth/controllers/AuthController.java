package ru.polyshelf.auth.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.auth.dto.AuthCredentials;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.dto.ChangePasswordRequest;
import ru.polyshelf.auth.dto.RefreshTokenRequest;
import ru.polyshelf.auth.dto.YandexAuthRequest;
import ru.polyshelf.auth.services.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthTokensResponse> register(@Valid @RequestBody AuthCredentials request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthTokensResponse login(@Valid @RequestBody AuthCredentials request) {
        return authService.login(request);
    }

    @PostMapping("/yandex")
    public AuthTokensResponse yandex(@Valid @RequestBody YandexAuthRequest request) {
        return authService.yandex(request);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authorization, request.currentPassword(), request.newPassword());
    }

    @PostMapping("/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

}
