package ru.polyshelf.gateway.auth.exception;

import org.springframework.http.HttpStatus;
import ru.polyshelf.gateway.common.error.GatewayException;

public class InvalidCredentialsException extends GatewayException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
