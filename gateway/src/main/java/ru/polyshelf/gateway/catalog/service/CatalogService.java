package ru.polyshelf.gateway.catalog.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.polyshelf.gateway.catalog.client.CatalogClient;
import ru.polyshelf.gateway.catalog.client.NewProductPayload;
import ru.polyshelf.gateway.common.error.UpstreamContractException;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final String CATALOG = "catalog";

    private final CatalogClient catalogClient;

    public Optional<Map<String, Object>> findProductByCode(String code) {
        return catalogClient.findProductByCode(code);
    }

    public Map<String, Object> registerNewProduct(int userId, String barcode, String name, LocalDate expirationDate) {
        return catalogClient.createProduct(new NewProductPayload(userId, barcode.trim(), name.trim(), expirationDate))
                .orElseThrow(() -> new UpstreamContractException(CATALOG, "empty product information"));
    }
}
