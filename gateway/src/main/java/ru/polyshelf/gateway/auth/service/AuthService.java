package ru.polyshelf.gateway.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.polyshelf.gateway.auth.client.IdentityClient;
import ru.polyshelf.gateway.auth.client.IdentitySession;
import ru.polyshelf.gateway.auth.web.LoginRequest;
import ru.polyshelf.gateway.auth.web.RefreshTokenRequest;
import ru.polyshelf.gateway.auth.web.RegisterRequest;
import ru.polyshelf.gateway.auth.web.TokenResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IdentityClient identityClient;
    private final AccessTokenService accessTokenService;

    public TokenResponse register(RegisterRequest request) {
        return toTokens(identityClient.register(request.email(), request.password()));
    }

    public TokenResponse login(LoginRequest request) {
        return toTokens(identityClient.login(request.email(), request.password()));
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        return toTokens(identityClient.refresh(request.refreshToken()));
    }

    public void logout(RefreshTokenRequest request) {
        identityClient.logout(request.refreshToken());
    }

    private TokenResponse toTokens(IdentitySession session) {
        AccessToken accessToken = accessTokenService.issue(session.userId(), session.email());
        return TokenResponse.bearer(
                accessToken.value(),
                accessToken.expiresAt(),
                session.refreshToken(),
                session.refreshTokenExpiresAt());
    }
}
