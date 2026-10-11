package ru.polyshelf.gateway.catalog.client;

import java.time.LocalDate;

public record NewProductPayload(int userId, String barcode, String name, LocalDate expirationDate) {
}
