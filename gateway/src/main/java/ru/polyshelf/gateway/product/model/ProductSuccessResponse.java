package ru.polyshelf.gateway.product.model;

import java.util.Map;

public record ProductSuccessResponse(String status, Map<String, Object> product) {

    private static final String SUCCESS = "SUCCESS";

    public static ProductSuccessResponse of(Map<String, Object> product) {
        return new ProductSuccessResponse(SUCCESS, product);
    }
}
