package ru.polyshelf.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import ru.polyshelf.gateway.common.error.ProblemResponses;

public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    static final String TOKEN_INVALID = "TOKEN_INVALID";
    static final String UNAUTHORIZED = "UNAUTHORIZED";

    private final BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        bearer.commence(request, response, exception);
        ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, codeOf(exception), "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        ProblemResponses.write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied");
    }

    static String codeOf(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth) {
            String description = oauth.getError().getDescription();
            if (description != null && description.toLowerCase(Locale.ROOT).contains("expired")) {
                return TOKEN_EXPIRED;
            }
            return TOKEN_INVALID;
        }
        return UNAUTHORIZED;
    }
}
