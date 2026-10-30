package ru.polyshelf.datamatrix.dto;

public record CzApiResponse(
        String productName,
        Long expireDate
) {
}