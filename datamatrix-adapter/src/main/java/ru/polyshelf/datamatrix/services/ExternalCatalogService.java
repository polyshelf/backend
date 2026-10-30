package ru.polyshelf.datamatrix.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.datamatrix.dto.CzApiRequest;
import ru.polyshelf.datamatrix.dto.CzApiResponse;
import ru.polyshelf.datamatrix.dto.DatamatrixRequest;
import ru.polyshelf.datamatrix.dto.ProductInfoResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class ExternalCatalogService {

    private final RestClient externalApiRestClient;

    public ProductInfoResponse getCzInfo(DatamatrixRequest request) {
        String code = formatCode(request.code());

        CzApiResponse czApiResponse = externalApiRestClient.post()
                .uri("/v2/mobile/check")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CzApiRequest(code))
                .retrieve()
                .body(CzApiResponse.class);

        if (czApiResponse == null
                || czApiResponse.productName() == null
                || czApiResponse.productName().isBlank()
                || czApiResponse.expireDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from external API");
        }

        return new ProductInfoResponse(
                czApiResponse.productName(),
                parseTimestamp(czApiResponse.expireDate())
        );
    }

    private String formatCode(String code) {
        return code.replace(" ", "\u001d");
    }

    private LocalDate parseTimestamp(Long timestamp) {
        if (timestamp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Missing timestamp in external API response");
        }

        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }

}