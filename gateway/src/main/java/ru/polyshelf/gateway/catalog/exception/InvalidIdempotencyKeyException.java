package ru.polyshelf.gateway.catalog.exception;

import org.springframework.http.HttpStatus;
import ru.polyshelf.gateway.common.error.GatewayException;

public class InvalidIdempotencyKeyException extends GatewayException {

    public InvalidIdempotencyKeyException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                "Idempotency-Key must match [A-Za-z0-9_-]{1,128}");
    }
}
