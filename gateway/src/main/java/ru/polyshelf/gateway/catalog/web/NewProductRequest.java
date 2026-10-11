package ru.polyshelf.gateway.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NewProductRequest(
        @Size(max = 32) String barcode,
        @NotBlank @Size(max = 255) String name,
        @NotNull LocalDate expirationDate
) {
}
