package ru.polyshelf.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class AuthApiException extends ResponseStatusException {

    private final AuthErrorCode code;

    public AuthApiException(HttpStatus status, AuthErrorCode code, String message) {
        super(status, message);
        this.code = code;
    }

}
