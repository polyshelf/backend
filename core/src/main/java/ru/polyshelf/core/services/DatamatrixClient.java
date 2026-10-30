package ru.polyshelf.core.services;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.dto.DatamatrixRequest;
import ru.polyshelf.core.dto.DatamatrixResponse;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class DatamatrixClient {

    private final RestClient restClient;

    public DatamatrixClient(@Qualifier("datamatrixRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public DatamatrixResponse getProductInfo(DatamatrixRequest request) {
        DatamatrixResponse response;

        try {
            response = restClient.post()
                    .uri("/catalog/datamatrix")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DatamatrixResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "Datamatrix adapter returned HTTP " + e.getStatusCode().value(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Datamatrix adapter is unavailable", e);
        }

        if (response == null
                || response.name() == null
                || response.name().isBlank()
                || response.expirationDate() == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "Invalid response from datamatrix adapter");
        }

        return response;
    }

}