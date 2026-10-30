package ru.polyshelf.core.dto;

import jakarta.validation.constraints.NotBlank;

public record DatamatrixRequest(
        @NotBlank String code
) {
}