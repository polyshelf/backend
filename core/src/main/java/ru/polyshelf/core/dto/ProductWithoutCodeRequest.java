package ru.polyshelf.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProductWithoutCodeRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        @JsonProperty("expiration_date")
        LocalDate expirationDate
) {
}
