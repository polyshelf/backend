package ru.polyshelf.datamatrix.dto;

import jakarta.validation.constraints.NotBlank;

public record DatamatrixRequest(
        @NotBlank String code
) {
}