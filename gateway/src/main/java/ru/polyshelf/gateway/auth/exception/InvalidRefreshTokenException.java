package ru.polyshelf.gateway.auth.exception;

import org.springframework.http.HttpStatus;
import ru.polyshelf.gateway.common.error.GatewayException;

public class InvalidRefreshTokenException extends GatewayException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token");
    }
}
