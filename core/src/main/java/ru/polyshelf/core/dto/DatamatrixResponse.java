package ru.polyshelf.core.dto;

import java.time.LocalDate;

public record DatamatrixResponse(
        String name,
        LocalDate expirationDate
) {
}