package ru.polyshelf.gateway.product.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProductBarcodeRequest(
        @NotBlank @Size(max = 32) String code,
        @JsonProperty("expiration_date") @NotNull LocalDate expirationDate
) {
}
