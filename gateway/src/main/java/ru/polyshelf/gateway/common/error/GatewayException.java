package ru.polyshelf.gateway.common.error;

import org.springframework.http.HttpStatus;

public abstract class GatewayException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected GatewayException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
