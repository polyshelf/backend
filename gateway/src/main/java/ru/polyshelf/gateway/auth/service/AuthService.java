package ru.polyshelf.gateway.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.polyshelf.gateway.auth.client.IdentityClient;
import ru.polyshelf.gateway.auth.client.IdentitySession;
import ru.polyshelf.gateway.auth.web.AuthCredentials;
import ru.polyshelf.gateway.auth.web.AuthTokensResponse;
import ru.polyshelf.gateway.auth.web.RefreshTokenRequest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IdentityClient identityClient;
    private final AccessTokenService accessTokenService;

    public AuthTokensResponse register(AuthCredentials credentials) {
        return toTokens(identityClient.register(credentials.email(), credentials.password()));
    }

    public AuthTokensResponse login(AuthCredentials credentials) {
        return toTokens(identityClient.login(credentials.email(), credentials.password()));
    }

    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        return toTokens(identityClient.refresh(request.refreshToken()));
    }

    private AuthTokensResponse toTokens(IdentitySession session) {
        AccessToken accessToken = accessTokenService.issue(session.userId(), session.email());
        return AuthTokensResponse.bearer(accessToken.value(), session.refreshToken(), accessToken.expiresInSeconds());
    }
}
