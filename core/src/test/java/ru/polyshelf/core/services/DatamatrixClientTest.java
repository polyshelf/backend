package ru.polyshelf.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.dto.DatamatrixRequest;
import ru.polyshelf.core.dto.DatamatrixResponse;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DatamatrixClientTest {

    private MockRestServiceServer server;
    private DatamatrixClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8083");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DatamatrixClient(builder.build());
    }

    @Test
    void getProductInfo_shouldSendPostAndReadResponse() {
        server.expect(requestTo("http://localhost:8083/catalog/datamatrix"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("010460123456789021ABC"))
                .andRespond(withSuccess("""
                        {"name":"Молоко","expirationDate":"2026-11-01"}
                        """, MediaType.APPLICATION_JSON));

        DatamatrixResponse result = client.getProductInfo(
                new DatamatrixRequest("010460123456789021ABC"));

        assertAll(
                () -> assertEquals("Молоко", result.name()),
                () -> assertEquals(LocalDate.of(2026, Month.NOVEMBER, 1), result.expirationDate())
        );
        server.verify();
    }

    @Test
    void getProductInfo_shouldReturn502WhenAdapterFails() {
        server.expect(requestTo("http://localhost:8083/catalog/datamatrix"))
                .andRespond(withServerError());

        DatamatrixRequest request = new DatamatrixRequest("123");
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.getProductInfo(request)
        );

        assertEquals(BAD_GATEWAY, exception.getStatusCode());
        server.verify();
    }

    @Test
    void getProductInfo_shouldReturn502WhenAdapterReturnsInvalidData() {
        server.expect(requestTo("http://localhost:8083/catalog/datamatrix"))
                .andRespond(withSuccess("""
                        {"name":"","expirationDate":"2026-11-01"}
                        """, MediaType.APPLICATION_JSON));

        DatamatrixRequest request = new DatamatrixRequest("123");
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.getProductInfo(request)
        );

        assertEquals(BAD_GATEWAY, exception.getStatusCode());
        server.verify();
    }
}
