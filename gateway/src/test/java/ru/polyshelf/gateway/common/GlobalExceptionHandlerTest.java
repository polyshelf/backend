package ru.polyshelf.gateway.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import ru.polyshelf.gateway.auth.exception.InvalidCredentialsException;
import ru.polyshelf.gateway.common.error.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void gatewayExceptionKeepsStatusAndCode() {
        ProblemDetail problem = handler.handleGatewayException(new InvalidCredentialsException());

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_CREDENTIALS");
    }

    @Test
    void distinguishesTimeoutFromUnavailable() {
        ProblemDetail timeout = handler.handleUpstreamUnavailable(
                new ResourceAccessException("x", new IOException(new HttpTimeoutException("read"))));
        ProblemDetail connect = handler.handleUpstreamUnavailable(
                new ResourceAccessException("x", new HttpConnectTimeoutException("connect")));

        assertThat(timeout.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(connect.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    void passesThroughClientErrorsAndMasksServerErrors() {
        ProblemDetail conflict = handler.handleUpstreamResponse(new HttpClientErrorException(HttpStatus.CONFLICT));
        ProblemDetail forbidden = handler.handleUpstreamResponse(new HttpClientErrorException(HttpStatus.FORBIDDEN));
        ProblemDetail failure = handler.handleUpstreamResponse(
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(conflict.getStatus()).isEqualTo(409);
        assertThat(conflict.getProperties()).containsEntry("code", "UPSTREAM_REJECTED");
        assertThat(forbidden.getStatus()).isEqualTo(502);
        assertThat(failure.getStatus()).isEqualTo(502);
        assertThat(handler.handleUpstreamFailure(new RestClientException("boom")).getStatus()).isEqualTo(502);
    }
}
