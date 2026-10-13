package ru.polyshelf.gateway.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class GatewayException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected GatewayException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
