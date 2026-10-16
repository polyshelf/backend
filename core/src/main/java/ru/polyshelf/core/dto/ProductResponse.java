package ru.polyshelf.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        @JsonProperty("expiration_date") LocalDate expirationDate
) {
}
