package ru.polyshelf.gateway.auth.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.polyshelf.gateway.auth.exception.EmailAlreadyRegisteredException;
import ru.polyshelf.gateway.auth.exception.InvalidCredentialsException;
import ru.polyshelf.gateway.auth.exception.InvalidRefreshTokenException;
import ru.polyshelf.gateway.common.config.HttpClientsConfiguration;
import ru.polyshelf.gateway.common.error.UpstreamContractException;

@Component
public class IdentityClient {

    private static final String SERVICE_NAME = "core";
    private static final String REGISTER_PATH = "/internal/auth/register";
    private static final String LOGIN_PATH = "/internal/auth/login";
    private static final String REFRESH_PATH = "/internal/auth/refresh";
    private static final String LOGOUT_PATH = "/internal/auth/logout";

    private final RestClient client;

    public IdentityClient(@Qualifier(HttpClientsConfiguration.CORE_CLIENT) RestClient client) {
        this.client = client;
    }

    public IdentitySession register(String email, String password) {
        try {
            return exchange(REGISTER_PATH, new CredentialsPayload(email, password));
        } catch (HttpClientErrorException.Conflict e) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    public IdentitySession login(String email, String password) {
        try {
            return exchange(LOGIN_PATH, new CredentialsPayload(email, password));
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidCredentialsException();
        }
    }

    public IdentitySession refresh(String refreshToken) {
        try {
            return exchange(REFRESH_PATH, new RefreshTokenPayload(refreshToken));
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidRefreshTokenException();
        }
    }

    public void logout(String refreshToken) {
        client.post()
                .uri(LOGOUT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshTokenPayload(refreshToken))
                .retrieve()
                .toBodilessEntity();
    }

    private IdentitySession exchange(String path, Object payload) {
        IdentitySession session = client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(IdentitySession.class);
        if (session == null) {
            throw new UpstreamContractException(SERVICE_NAME, "empty identity session");
        }
        return session;
    }
}
