package ru.polyshelf.datamatrix.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.datamatrix.dto.DatamatrixRequest;
import ru.polyshelf.datamatrix.dto.ProductInfoResponse;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalCatalogServiceTest {

    private MockRestServiceServer server;
    private ExternalCatalogService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.example.com");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new ExternalCatalogService(builder.build());
    }

    @Test
    void getCzInfo_shouldSendPostAndMapResponse() {
        long expirationTimestamp = Instant.parse("2026-11-01T12:00:00Z").toEpochMilli();

        server.expect(requestTo("https://api.example.com/v2/mobile/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("010460123456789021ABC"))
                .andExpect(jsonPath("$.codeType").value("datamatrix"))
                .andRespond(withSuccess("""
                        {"productName":"Молоко","expireDate":%d}
                        """.formatted(expirationTimestamp), MediaType.APPLICATION_JSON));

        ProductInfoResponse result = service.getCzInfo(
                new DatamatrixRequest("010460123456789021ABC"));

        assertAll(
                () -> assertEquals("Молоко", result.name()),
                () -> assertEquals(LocalDate.of(2026, 11, 1), result.expirationDate())
        );
        server.verify();
    }

    @Test
    void getCzInfo_shouldReplaceSpacesWithGroupSeparator() {
        server.expect(requestTo("https://api.example.com/v2/mobile/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.code").value("0104601234567890\u001d21ABC"))
                .andRespond(withSuccess("""
                        {"productName":"Молоко","expireDate":1793534400000}
                        """, MediaType.APPLICATION_JSON));

        service.getCzInfo(new DatamatrixRequest("0104601234567890 21ABC"));

        server.verify();
    }

    @Test
    void getCzInfo_shouldReturn502WhenRequiredFieldIsMissing() {
        server.expect(requestTo("https://api.example.com/v2/mobile/check"))
                .andRespond(withSuccess("""
                        {"productName":"Молоко"}
                        """, MediaType.APPLICATION_JSON));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getCzInfo(new DatamatrixRequest("123"))
        );

        assertEquals(BAD_GATEWAY, exception.getStatusCode());
        server.verify();
    }

    @Test
    void getCzInfo_shouldReturn502WhenBodyIsEmpty() {
        server.expect(requestTo("https://api.example.com/v2/mobile/check"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getCzInfo(new DatamatrixRequest("123"))
        );

        assertEquals(BAD_GATEWAY, exception.getStatusCode());
        server.verify();
    }
}
