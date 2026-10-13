package ru.polyshelf.gateway.common.error;

import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";

    private static final Set<Integer> PASS_THROUGH = Set.of(400, 404, 409, 422, 429);

    @ExceptionHandler(GatewayException.class)
    public ProblemDetail handleGatewayException(GatewayException exception) {
        if (exception.getStatus().is5xxServerError()) {
            log.error("Gateway error: {}", exception.getMessage());
        }
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleUpstreamUnavailable(ResourceAccessException exception) {
        log.error("Upstream service is unavailable", exception);
        if (isReadTimeout(exception)) {
            return problem(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT", "Upstream service timed out");
        }
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", "Upstream service is unavailable");
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail handleUpstreamResponse(RestClientResponseException exception) {
        int upstreamStatus = exception.getStatusCode().value();
        if (PASS_THROUGH.contains(upstreamStatus)) {
            return problem(HttpStatus.valueOf(upstreamStatus), "UPSTREAM_REJECTED",
                    "Request was rejected by upstream service");
        }
        log.error("Upstream service returned {}", upstreamStatus, exception);
        return problem(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", "Upstream service returned an error");
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleUpstreamFailure(RestClientException exception) {
        log.error("Upstream service failure", exception);
        return problem(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", "Upstream service returned an error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request body");
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    static boolean isReadTimeout(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpConnectTimeoutException) {
                return false;
            }
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
