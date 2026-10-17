package ru.polyshelf.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProductWithBarcodeRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "\\d{13}")
        String code,

        @NotNull
        @JsonProperty("expiration_date")
        LocalDate expirationDate
) {
}
