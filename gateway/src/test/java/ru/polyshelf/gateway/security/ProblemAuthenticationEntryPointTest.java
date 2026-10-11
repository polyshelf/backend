package ru.polyshelf.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class ProblemAuthenticationEntryPointTest {

    private final ProblemAuthenticationEntryPoint entryPoint = new ProblemAuthenticationEntryPoint();

    @Test
    void reportsExpiredToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InvalidBearerTokenException("Jwt expired at 2026-01-01T00:00:00Z"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"TOKEN_EXPIRED\"");
        assertThat(response.getHeader("WWW-Authenticate")).startsWith("Bearer");
    }

    @Test
    void reportsInvalidAndMissingToken() throws Exception {
        MockHttpServletResponse invalid = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), invalid, new InvalidBearerTokenException("bad signature"));
        MockHttpServletResponse missing = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), missing, new InsufficientAuthenticationException("none"));

        assertThat(invalid.getContentAsString()).contains("TOKEN_INVALID");
        assertThat(missing.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    @Test
    void reportsAccessDenied() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.handle(new MockHttpServletRequest(), response, new AccessDeniedException("no"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCESS_DENIED");
    }
}
