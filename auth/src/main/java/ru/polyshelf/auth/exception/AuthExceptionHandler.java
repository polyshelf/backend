package ru.polyshelf.auth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthApiException.class)
    public ResponseEntity<Map<String, Object>> handle(AuthApiException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "code", ex.getCode().name(),
                "message", String.valueOf(ex.getReason())));
    }

}
