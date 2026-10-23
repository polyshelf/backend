package ru.polyshelf.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record YandexAuthRequest(
        @NotBlank
        @Size(max = 2048)
        @JsonProperty("access_token")
        String accessToken,

        @Size(max = 72)
        @JsonProperty("link_password")
        String linkPassword
) {
}
