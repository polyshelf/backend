package ru.polyshelf.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Size(max = 72)
        @JsonProperty("current_password")
        String currentPassword,

        @NotBlank
        @Size(min = 6, max = 72)
        @JsonProperty("new_password")
        String newPassword
) {
}
