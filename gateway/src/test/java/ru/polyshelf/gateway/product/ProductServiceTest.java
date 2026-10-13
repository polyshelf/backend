package ru.polyshelf.gateway.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.polyshelf.gateway.catalog.service.CatalogService;
import ru.polyshelf.gateway.common.error.UpstreamContractException;
import ru.polyshelf.gateway.product.client.InventoryClient;
import ru.polyshelf.gateway.product.exception.CodeNotResolvedException;
import ru.polyshelf.gateway.product.model.ProductSuccessResponse;
import ru.polyshelf.gateway.product.service.ProductService;
import ru.polyshelf.gateway.product.web.ProductBarcodeCandidateRequest;
import ru.polyshelf.gateway.product.web.ProductBarcodeRequest;
import ru.polyshelf.gateway.scan.client.CodeAdapterClient;
import ru.polyshelf.gateway.scan.client.CodeAdapterOutcome;
import ru.polyshelf.gateway.scan.client.CodeAdapterResult;
import ru.polyshelf.gateway.scan.model.PreparedRequest;

class ProductServiceTest {

    private static final LocalDate DATE = LocalDate.of(2030, Month.JANUARY, 1);
    private static final Map<String, Object> SAVED = Map.of("id", "1", "name", "Milk");

    private final CodeAdapterClient adapter = mock(CodeAdapterClient.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final InventoryClient inventory = mock(InventoryClient.class);
    private final ProductService service = new ProductService(adapter, catalog, inventory);

    @Test
    void dataMatrixProductIsStoredInInventory() {
        when(adapter.process("dm")).thenReturn(new CodeAdapterResult(CodeAdapterOutcome.PRODUCT_FOUND,
                Map.of("name", "Milk", "expirationDate", "2030-01-01"), null, null));
        when(inventory.add(5, "Milk", DATE)).thenReturn(ProductSuccessResponse.of(SAVED));

        ProductSuccessResponse response = service.addFromDataMatrix(5, "dm");

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.product()).isEqualTo(SAVED);
    }

    @Test
    void dataMatrixFailsWhenCodeCannotBeResolved() {
        when(adapter.process("dm")).thenReturn(new CodeAdapterResult(CodeAdapterOutcome.CLIENT_FETCH_REQUIRED,
                null, new PreparedRequest("u", Map.of()), null));
        assertThatThrownBy(() -> service.addFromDataMatrix(5, "dm")).isInstanceOf(CodeNotResolvedException.class);

        when(adapter.process("bc")).thenReturn(new CodeAdapterResult(CodeAdapterOutcome.BARCODE, null, null, "4601"));
        assertThatThrownBy(() -> service.addFromDataMatrix(5, "bc")).isInstanceOf(CodeNotResolvedException.class);
    }

    @Test
    void dataMatrixFailsOnContractViolation() {
        when(adapter.process("dm")).thenReturn(new CodeAdapterResult(CodeAdapterOutcome.PRODUCT_FOUND,
                Map.of("name", "Milk"), null, null));

        assertThatThrownBy(() -> service.addFromDataMatrix(5, "dm")).isInstanceOf(UpstreamContractException.class);
        verify(inventory, never()).add(anyInt(), any(), any());
    }

    @Test
    void barcodeProductFromCatalogIsStoredInInventory() {
        when(catalog.findProductByCode("4601")).thenReturn(Optional.of(Map.of("name", "Milk")));
        when(inventory.add(5, "Milk", DATE)).thenReturn(ProductSuccessResponse.of(SAVED));

        Optional<ProductSuccessResponse> response = service.addFromBarcode(5, new ProductBarcodeRequest(" 4601 ", DATE));

        assertThat(response).isPresent();
    }

    @Test
    void unknownBarcodeIsReportedAsMissing() {
        when(catalog.findProductByCode("4601")).thenReturn(Optional.empty());

        assertThat(service.addFromBarcode(5, new ProductBarcodeRequest("4601", DATE))).isEmpty();
        verify(inventory, never()).add(anyInt(), any(), any());
    }

    @Test
    void candidateIsRegisteredInCatalog() {
        when(catalog.registerNewProduct(5, "4601", "Milk", DATE)).thenReturn(SAVED);

        ProductSuccessResponse response =
                service.addFromBarcodeCandidate(5, new ProductBarcodeCandidateRequest("4601", "Milk", DATE));

        assertThat(response.product()).isEqualTo(SAVED);
    }
}
