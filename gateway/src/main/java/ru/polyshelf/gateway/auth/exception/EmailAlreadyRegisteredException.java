package ru.polyshelf.gateway.auth.exception;

import org.springframework.http.HttpStatus;
import ru.polyshelf.gateway.common.error.GatewayException;

public class EmailAlreadyRegisteredException extends GatewayException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
    }
}
