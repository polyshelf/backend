package ru.polyshelf.gateway.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(@JsonProperty("refresh_token") @NotBlank @Size(max = 512) String refreshToken) {
}
