package ru.polyshelf.gateway.product.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

record InventoryRequest(String name, @JsonProperty("expiration_date") LocalDate expirationDate) {
}
