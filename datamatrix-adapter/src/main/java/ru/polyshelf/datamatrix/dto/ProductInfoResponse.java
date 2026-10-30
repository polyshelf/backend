package ru.polyshelf.datamatrix.dto;

import java.time.LocalDate;

public record ProductInfoResponse(
        String name,
        LocalDate expirationDate
) {
}