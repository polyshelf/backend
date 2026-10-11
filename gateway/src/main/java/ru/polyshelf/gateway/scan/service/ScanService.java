package ru.polyshelf.gateway.scan.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.polyshelf.gateway.catalog.service.CatalogService;
import ru.polyshelf.gateway.common.error.UpstreamContractException;
import ru.polyshelf.gateway.scan.client.CodeAdapterClient;
import ru.polyshelf.gateway.scan.client.CodeAdapterOutcome;
import ru.polyshelf.gateway.scan.client.CodeAdapterResult;
import ru.polyshelf.gateway.scan.model.ScanResult;

@Service
@RequiredArgsConstructor
public class ScanService {

    private static final String CODE_ADAPTER = "code-adapter";

    private final CodeAdapterClient codeAdapterClient;
    private final CatalogService catalogService;

    public ScanResult scan(String code) {
        CodeAdapterResult result = codeAdapterClient.process(code);
        CodeAdapterOutcome outcome = require(result.outcome(), "outcome");

        return switch (outcome) {
            case PRODUCT_FOUND -> ScanResult.productFound(require(result.product(), "product"));
            case CLIENT_FETCH_REQUIRED -> ScanResult.clientFetchRequired(
                    require(result.clientRequest(), "clientRequest"));
            case BARCODE -> lookupInCatalog(requireText(result.barcode(), "barcode"));
        };
    }

    public ScanResult acceptCzResponse(Map<String, Object> czPayload) {
        return ScanResult.productFound(codeAdapterClient.prepareProductForApp(czPayload));
    }

    private ScanResult lookupInCatalog(String barcode) {
        return catalogService.findProductByCode(barcode)
                .map(ScanResult::productFound)
                .orElseGet(() -> ScanResult.userInputRequired(barcode));
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new UpstreamContractException(CODE_ADAPTER, "missing field '" + field + "'");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new UpstreamContractException(CODE_ADAPTER, "missing field '" + field + "'");
        }
        return value;
    }
}
