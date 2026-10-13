package ru.polyshelf.gateway.auth.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record AuthCredentials(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 72) String password
) {

    private static final int BCRYPT_MAX_BYTES = 72;

    public AuthCredentials {
        email = Emails.normalize(email);
    }

    @JsonIgnore
    @AssertTrue(message = "password must not exceed 72 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
