package ru.polyshelf.gateway.product.client;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.polyshelf.gateway.common.client.InternalHeaders;
import ru.polyshelf.gateway.common.config.HttpClientsConfiguration;
import ru.polyshelf.gateway.common.error.UpstreamContractException;
import ru.polyshelf.gateway.product.model.ProductSuccessResponse;

@Component
public class InventoryClient {

    private static final String SERVICE_NAME = "core";
    private static final String PRODUCTS_PATH = "/products";

    private final RestClient client;

    public InventoryClient(@Qualifier(HttpClientsConfiguration.CORE_CLIENT) RestClient client) {
        this.client = client;
    }

    public ProductSuccessResponse add(int userId, String name, LocalDate expirationDate) {
        ProductSuccessResponse response = client.post()
                .uri(PRODUCTS_PATH)
                .header(InternalHeaders.USER_ID, Integer.toString(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InventoryRequest(name, expirationDate))
                .retrieve()
                .body(ProductSuccessResponse.class);
        if (response == null || response.product() == null) {
            throw new UpstreamContractException(SERVICE_NAME, "empty product information");
        }
        return response;
    }
}
