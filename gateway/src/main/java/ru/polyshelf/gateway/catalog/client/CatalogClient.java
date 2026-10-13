package ru.polyshelf.gateway.catalog.client;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.polyshelf.gateway.common.config.HttpClientsConfiguration;

@Component
public class CatalogClient {

    private static final String PRODUCT_BY_CODE_PATH = "/product-by-code?code={code}";
    private static final String NEW_PRODUCT_PATH = "/new-product";
    private static final ParameterizedTypeReference<Map<String, Object>> PRODUCT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient client;

    public CatalogClient(@Qualifier(HttpClientsConfiguration.CATALOG_CLIENT) RestClient client) {
        this.client = client;
    }

    public Optional<Map<String, Object>> findProductByCode(String code) {
        try {
            return Optional.ofNullable(client.get()
                    .uri(PRODUCT_BY_CODE_PATH, code)
                    .retrieve()
                    .body(PRODUCT_TYPE));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> createProduct(NewProductPayload payload) {
        return Optional.ofNullable(client.post()
                .uri(NEW_PRODUCT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(PRODUCT_TYPE));
    }
}
