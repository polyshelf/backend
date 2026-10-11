package ru.polyshelf.gateway.scan.client;

import java.util.Map;
import ru.polyshelf.gateway.scan.model.PreparedRequest;

public record CodeAdapterResult(
        CodeAdapterOutcome outcome,
        Map<String, Object> product,
        PreparedRequest clientRequest,
        String barcode
) {
}
