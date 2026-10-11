package ru.polyshelf.gateway.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 72) String password
) {

    public LoginRequest {
        email = Emails.normalize(email);
    }
}
