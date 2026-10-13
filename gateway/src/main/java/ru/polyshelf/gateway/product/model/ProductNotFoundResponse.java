package ru.polyshelf.gateway.product.model;

public record ProductNotFoundResponse(String status, String code) {

    private static final String NOT_FOUND = "NOT_FOUND";

    public static ProductNotFoundResponse of(String code) {
        return new ProductNotFoundResponse(NOT_FOUND, code);
    }
}
