package ru.polyshelf.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String name,
        @JsonProperty("expiration_date") LocalDate expirationDate,
        @JsonProperty("consumed_at") OffsetDateTime consumedAt,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {
}
