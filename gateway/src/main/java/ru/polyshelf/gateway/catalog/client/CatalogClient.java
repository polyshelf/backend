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

    private static final String PRODUCT_BY_CODE = "product-by-code";
    private static final String NEW_PRODUCT = "new-product";
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
                    .uri(uri -> uri.pathSegment(PRODUCT_BY_CODE).queryParam("code", "{code}").build(code))
                    .retrieve()
                    .body(PRODUCT_TYPE));
        } catch (HttpClientErrorException.NotFound _) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> createProduct(NewProductPayload payload) {
        return Optional.ofNullable(client.post()
                .uri(uri -> uri.pathSegment(NEW_PRODUCT).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(PRODUCT_TYPE));
    }
}
