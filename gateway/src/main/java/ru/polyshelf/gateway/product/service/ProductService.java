package ru.polyshelf.gateway.product.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.polyshelf.gateway.catalog.service.CatalogService;
import ru.polyshelf.gateway.common.error.UpstreamContractException;
import ru.polyshelf.gateway.product.client.InventoryClient;
import ru.polyshelf.gateway.product.exception.CodeNotResolvedException;
import ru.polyshelf.gateway.product.model.ProductSuccessResponse;
import ru.polyshelf.gateway.product.web.ProductBarcodeCandidateRequest;
import ru.polyshelf.gateway.product.web.ProductBarcodeRequest;
import ru.polyshelf.gateway.scan.client.CodeAdapterClient;
import ru.polyshelf.gateway.scan.client.CodeAdapterResult;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);
    private static final String CODE_ADAPTER = "code-adapter";
    private static final String CATALOG = "catalog";
    private static final String NAME = "name";
    private static final String EXPIRATION_DATE = "expirationDate";

    private final CodeAdapterClient codeAdapterClient;
    private final CatalogService catalogService;
    private final InventoryClient inventoryClient;

    public ProductSuccessResponse addFromDataMatrix(int userId, String code) {
        CodeAdapterResult result = codeAdapterClient.process(code);
        if (result.outcome() == null) {
            throw new UpstreamContractException(CODE_ADAPTER, "missing field 'outcome'");
        }
        return switch (result.outcome()) {
            case PRODUCT_FOUND -> {
                Map<String, Object> product = result.product();
                if (product == null) {
                    throw new UpstreamContractException(CODE_ADAPTER, "missing field 'product'");
                }
                yield inventoryClient.add(userId,
                        text(product, NAME, CODE_ADAPTER),
                        date(product));
            }
            case CLIENT_FETCH_REQUIRED -> throw new CodeNotResolvedException(
                    HttpStatus.SERVICE_UNAVAILABLE, "CODE_SERVICE_UNAVAILABLE", "Code could not be resolved right now");
            case BARCODE -> throw new CodeNotResolvedException(
                    UNPROCESSABLE, "UNSUPPORTED_CODE", "Code is not a DataMatrix product code");
        };
    }

    public Optional<ProductSuccessResponse> addFromBarcode(int userId, ProductBarcodeRequest request) {
        return catalogService.findProductByCode(request.code().trim())
                .map(product -> inventoryClient.add(userId, text(product, NAME, CATALOG), request.expirationDate()));
    }

    public ProductSuccessResponse addFromBarcodeCandidate(int userId, ProductBarcodeCandidateRequest request) {
        return ProductSuccessResponse.of(catalogService.registerNewProduct(
                userId, request.barcode(), request.name(), request.expirationDate()));
    }

    private static String text(Map<String, Object> product, String field, String service) {
        Object value = product.get(field);
        if (!(value instanceof String text) || !StringUtils.hasText(text)) {
            throw new UpstreamContractException(service, "missing field '" + field + "'");
        }
        return text;
    }

    private static LocalDate date(Map<String, Object> product) {
        try {
            return LocalDate.parse(text(product, EXPIRATION_DATE, CODE_ADAPTER));
        } catch (DateTimeParseException _) {
            throw new UpstreamContractException(CODE_ADAPTER, "invalid field '" + EXPIRATION_DATE + "'");
        }
    }
}
