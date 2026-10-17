package ru.polyshelf.core.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithBarcodeRequest;
import ru.polyshelf.core.dto.ProductWithBarcodeCandidateRequest;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.services.InventoryService;
import ru.polyshelf.core.security.UserPrincipal;
import ru.polyshelf.core.security.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;


    private static final UUID USER_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final UUID PRODUCT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(USER_ID, UserRole.USER);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addWithoutCode_shouldReturnCreatedProduct() throws Exception {

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithBarcode_shouldReturnCreatedProduct() throws Exception {

        String barcode = "4601234567890";
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);

        when(inventoryService.addWithCode(
                eq(USER_ID),
                any(ProductWithBarcodeRequest.class)
        )).thenReturn(
                new ProductResponse(
                        PRODUCT_ID,
                        "Молоко",
                        expirationDate
                )
        );

        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "4601234567890",
                              "expiration_date": "2026-10-20"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Молоко"))
                .andExpect(jsonPath("$.expiration_date")
                        .value("2026-10-20"));

        verify(inventoryService).addWithCode(
                USER_ID,
                new ProductWithBarcodeRequest(barcode, expirationDate)
        );
    }

    @Test
    void addWithBarcode_shouldReturn404WhenProductNotFound() throws Exception {

        when(inventoryService.addWithCode(
                eq(USER_ID),
                any(ProductWithBarcodeRequest.class)
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found in catalog"
                )
        );

        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "4601234567890",
                              "expiration_date": "2026-10-20"
                            }
                            """))
                .andExpect(status().isNotFound());

        verify(inventoryService).addWithCode(
                USER_ID,
                new ProductWithBarcodeRequest(
                        "4601234567890",
                        LocalDate.of(2026, Month.OCTOBER, 20)
                )
        );
    }

    @Test
    void addWithBarcode_shouldRejectBlankBarcode() throws Exception {
        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "",
                              "expiration_date": "2026-10-20"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithBarcode_shouldRejectInvalidBarcodeLength() throws Exception {
        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "46012345",
                              "expiration_date": "2026-10-20"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithBarcode_shouldRejectNonNumericBarcode() throws Exception {
        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "46012345678AB",
                              "expiration_date": "2026-10-20"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithBarcode_shouldRejectMissingExpirationDate() throws Exception {
        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "4601234567890"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithBarcode_shouldRejectInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/products/from-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "code": "4601234567890",
                              "expiration_date": "20-10-2026"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addWithBarcodeCandidate_shouldReturnCreatedProduct() throws Exception {
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);
        ProductWithBarcodeCandidateRequest request = new ProductWithBarcodeCandidateRequest(
                "4601234567890", "Молоко", expirationDate
        );
        ProductResponse response = new ProductResponse(PRODUCT_ID, "Молоко", expirationDate);

        when(inventoryService.addWithCodeCandidate(USER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/products/from-barcode-candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "4601234567890",
                                  "name": "Молоко",
                                  "expiration_date": "2026-10-20"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Молоко"))
                .andExpect(jsonPath("$.expiration_date").value("2026-10-20"));

        verify(inventoryService).addWithCodeCandidate(USER_ID, request);
    }

    @Test
    void addWithBarcodeCandidate_shouldReturn409WhenProductAlreadyExists() throws Exception {
        ProductWithBarcodeCandidateRequest request = new ProductWithBarcodeCandidateRequest(
                "4601234567890", "Молоко", LocalDate.of(2026, Month.OCTOBER, 20)
        );
        when(inventoryService.addWithCodeCandidate(USER_ID, request))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT, "Product already exists in catalog"
                ));

        mockMvc.perform(post("/products/from-barcode-candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "4601234567890",
                                  "name": "Молоко",
                                  "expiration_date": "2026-10-20"
                                }
                                """))
                .andExpect(status().isConflict());

        verify(inventoryService).addWithCodeCandidate(USER_ID, request);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectBlankBarcode() throws Exception {
        assertCandidateBadRequest("""
                {"code":"  ","name":"Молоко","expiration_date":"2026-10-20"}
                """);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectInvalidBarcodeLength() throws Exception {
        assertCandidateBadRequest("""
                {"code":"12345678","name":"Молоко","expiration_date":"2026-10-20"}
                """);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectNonNumericBarcode() throws Exception {
        assertCandidateBadRequest("""
                {"code":"46012345678AB","name":"Молоко","expiration_date":"2026-10-20"}
                """);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectMissingName() throws Exception {
        assertCandidateBadRequest("""
                {"code":"4601234567890","expiration_date":"2026-10-20"}
                """);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectBlankName() throws Exception {
        assertCandidateBadRequest("""
                {"code":"4601234567890","name":"  ","expiration_date":"2026-10-20"}
                """);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectTooLongName() throws Exception {
        String json = """
                {"code":"4601234567890","name":"%s","expiration_date":"2026-10-20"}
                """.formatted("A".repeat(256));
        assertCandidateBadRequest(json);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectMissingExpirationDate() throws Exception {
        assertCandidateBadRequest("""
                {"code":"4601234567890","name":"Молоко"}
                """);
    }

    @Test
    void addWithBarcodeCandidate_shouldRejectInvalidDateFormat() throws Exception {
        assertCandidateBadRequest("""
                {"code":"4601234567890","name":"Молоко","expiration_date":"20-10-2026"}
                """);
    }

    private void assertCandidateBadRequest(String json) throws Exception {
        mockMvc.perform(post("/products/from-barcode-candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inventoryService);
    }


    @Test
    void deleteFromInventory_shouldReturn204AndCallService() throws Exception {
        mockMvc.perform(delete("/products/{productId}", PRODUCT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(inventoryService).delete(USER_ID, PRODUCT_ID);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void deleteFromInventory_shouldReturn404WhenItemNotFoundOrNotOwned() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(inventoryService).delete(USER_ID, PRODUCT_ID);

        mockMvc.perform(delete("/products/{productId}", PRODUCT_ID))
                .andExpect(status().isNotFound());

        verify(inventoryService).delete(USER_ID, PRODUCT_ID);
    }

    @Test
    void deleteFromInventory_shouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(delete("/products/{productId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void markConsumed_shouldReturn204AndCallService() throws Exception {
        mockMvc.perform(put("/products/{itemId}/consumption", PRODUCT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(inventoryService).setConsumed(USER_ID, PRODUCT_ID, true);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void markNotConsumed_shouldReturn204AndCallService() throws Exception {
        mockMvc.perform(delete("/products/{itemId}/consumption", PRODUCT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(inventoryService).setConsumed(USER_ID, PRODUCT_ID, false);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void markConsumed_shouldReturn404WhenItemNotFoundOrNotOwned() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(inventoryService).setConsumed(USER_ID, PRODUCT_ID, true);

        mockMvc.perform(put("/products/{itemId}/consumption", PRODUCT_ID))
                .andExpect(status().isNotFound());

        verify(inventoryService).setConsumed(USER_ID, PRODUCT_ID, true);
    }

    @Test
    void markNotConsumed_shouldReturn404WhenItemNotFoundOrNotOwned() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(inventoryService).setConsumed(USER_ID, PRODUCT_ID, false);

        mockMvc.perform(delete("/products/{itemId}/consumption", PRODUCT_ID))
                .andExpect(status().isNotFound());

        verify(inventoryService).setConsumed(USER_ID, PRODUCT_ID, false);
    }

    @Test
    void markConsumed_shouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(put("/products/{itemId}/consumption", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void markNotConsumed_shouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(delete("/products/{itemId}/consumption", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

}
