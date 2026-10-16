package ru.polyshelf.core.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.services.InventoryService;
import ru.polyshelf.core.utils.DevJwtService;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private DevJwtService jwtService;

    private static final UUID USER_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final UUID PRODUCT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    void addWithoutCode_shouldReturnCreatedProduct() throws Exception {
        String authorization = "Bearer test-token";

        when(jwtService.extractUserId(authorization))
                .thenReturn(USER_ID);

        when(inventoryService.addWithoutCode(
                eq(USER_ID),
                any(ProductWithoutCodeRequest.class)
        )).thenReturn(
                new ProductResponse(
                        PRODUCT_ID,
                        "Клубника",
                        LocalDate.of(2026, Month.OCTOBER, 20)
                )
        );

        mockMvc.perform(post("/products")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Клубника",
                                  "expiration_date": "2026-10-20"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Клубника"))
                .andExpect(jsonPath("$.expiration_date")
                        .value("2026-10-20"));

        verify(jwtService).extractUserId(authorization);

        verify(inventoryService).addWithoutCode(
                USER_ID,
                new ProductWithoutCodeRequest(
                        "Клубника",
                        LocalDate.of(2026, Month.OCTOBER, 20)
                )
        );
    }

    @Test
    void addWithoutCode_shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "expiration_date": "2026-10-20"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithoutCode_shouldRejectMissingName() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expiration_date": "2026-10-20"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithoutCode_shouldRejectMissingExpirationDate() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Клубника"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithoutCode_shouldRejectInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Клубника",
                                  "expiration_date": "20-10-2026"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithoutCode_shouldRejectTooLongName() throws Exception {
        String longName = "A".repeat(256);

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "expiration_date": "2026-10-20"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithoutCode_shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithoutCode_shouldRequireAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Клубника",
                                  "expiration_date": "2026-10-20"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }
}
