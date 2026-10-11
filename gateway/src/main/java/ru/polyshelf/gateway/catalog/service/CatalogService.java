package ru.polyshelf.gateway.catalog.service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.polyshelf.gateway.catalog.client.CatalogClient;
import ru.polyshelf.gateway.catalog.client.NewProductPayload;
import ru.polyshelf.gateway.catalog.exception.InvalidIdempotencyKeyException;
import ru.polyshelf.gateway.catalog.web.NewProductRequest;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private final CatalogClient catalogClient;

    public Optional<Map<String, Object>> findProductByCode(String code) {
        return catalogClient.findProductByCode(code);
    }

    public void registerNewProduct(int userId, NewProductRequest request, String idempotencyKey) {
        String key = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null;
        if (key != null && !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        String barcode = StringUtils.hasText(request.barcode()) ? request.barcode().trim() : null;
        catalogClient.createProduct(
                new NewProductPayload(userId, barcode, request.name().trim(), request.expirationDate()),
                key);
    }
}
