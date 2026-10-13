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
    private static final String REGISTER = "register";
    private static final String LOGIN = "login";
    private static final String REFRESH = "refresh";

    private final RestClient client;

    public IdentityClient(@Qualifier(HttpClientsConfiguration.CORE_CLIENT) RestClient client) {
        this.client = client;
    }

    public IdentitySession register(String email, String password) {
        try {
            return exchange(REGISTER, new CredentialsPayload(email, password));
        } catch (HttpClientErrorException.Conflict _) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    public IdentitySession login(String email, String password) {
        try {
            return exchange(LOGIN, new CredentialsPayload(email, password));
        } catch (HttpClientErrorException.Unauthorized _) {
            throw new InvalidCredentialsException();
        }
    }

    public IdentitySession refresh(String refreshToken) {
        try {
            return exchange(REFRESH, new RefreshTokenPayload(refreshToken));
        } catch (HttpClientErrorException.Unauthorized _) {
            throw new InvalidRefreshTokenException();
        }
    }

    private IdentitySession exchange(String action, Object payload) {
        IdentitySession session = client.post()
                .uri(uri -> uri.pathSegment("internal", "auth", action).build())
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
