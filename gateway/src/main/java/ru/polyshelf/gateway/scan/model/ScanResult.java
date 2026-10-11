package ru.polyshelf.gateway.scan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScanResult(
        ScanStatus status,
        Map<String, Object> product,
        PreparedRequest clientRequest,
        String barcode
) {

    public static ScanResult productFound(Map<String, Object> product) {
        return new ScanResult(ScanStatus.PRODUCT_FOUND, product, null, null);
    }

    public static ScanResult clientFetchRequired(PreparedRequest clientRequest) {
        return new ScanResult(ScanStatus.CLIENT_FETCH_REQUIRED, null, clientRequest, null);
    }

    public static ScanResult userInputRequired(String barcode) {
        return new ScanResult(ScanStatus.USER_INPUT_REQUIRED, null, null, barcode);
    }
}
