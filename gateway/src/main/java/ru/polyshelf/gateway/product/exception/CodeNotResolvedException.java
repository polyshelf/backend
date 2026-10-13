package ru.polyshelf.gateway.product.exception;

import org.springframework.http.HttpStatus;
import ru.polyshelf.gateway.common.error.GatewayException;

public class CodeNotResolvedException extends GatewayException {

    public CodeNotResolvedException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
