package ru.polyshelf.gateway.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(@NotBlank @Size(max = 512) String refreshToken) {
}
