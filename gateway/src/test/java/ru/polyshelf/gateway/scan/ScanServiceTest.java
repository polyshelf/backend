package ru.polyshelf.gateway.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.polyshelf.gateway.catalog.service.CatalogService;
import ru.polyshelf.gateway.common.error.UpstreamContractException;
import ru.polyshelf.gateway.scan.client.CodeAdapterClient;
import ru.polyshelf.gateway.scan.client.CodeAdapterOutcome;
import ru.polyshelf.gateway.scan.client.CodeAdapterResult;
import ru.polyshelf.gateway.scan.model.PreparedRequest;
import ru.polyshelf.gateway.scan.model.ScanResult;
import ru.polyshelf.gateway.scan.model.ScanStatus;
import ru.polyshelf.gateway.scan.service.ScanService;

class ScanServiceTest {

    private final CodeAdapterClient adapter = mock(CodeAdapterClient.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final ScanService service = new ScanService(adapter, catalog);

    private static final Map<String, Object> PRODUCT = Map.of("name", "Milk");

    @Test
    void returnsProductFromCodeAdapter() {
        when(adapter.process("code")).thenReturn(
                new CodeAdapterResult(CodeAdapterOutcome.PRODUCT_FOUND, PRODUCT, null, null));

        ScanResult result = service.scan("code");

        assertThat(result.status()).isEqualTo(ScanStatus.PRODUCT_FOUND);
        assertThat(result.product()).isEqualTo(PRODUCT);
    }

    @Test
    void passesPreparedClientRequestThroughUnchanged() {
        PreparedRequest prepared = new PreparedRequest("https://api.example/check?c=1", Map.of("X", "y"));
        when(adapter.process("code")).thenReturn(
                new CodeAdapterResult(CodeAdapterOutcome.CLIENT_FETCH_REQUIRED, null, prepared, null));

        ScanResult result = service.scan("code");

        assertThat(result.status()).isEqualTo(ScanStatus.CLIENT_FETCH_REQUIRED);
        assertThat(result.clientRequest()).isEqualTo(prepared);
    }

    @Test
    void looksUpBarcodeInCatalog() {
        when(adapter.process("code")).thenReturn(
                new CodeAdapterResult(CodeAdapterOutcome.BARCODE, null, null, "4601"));
        when(catalog.findProductByCode("4601")).thenReturn(Optional.of(PRODUCT));

        assertThat(service.scan("code").status()).isEqualTo(ScanStatus.PRODUCT_FOUND);
    }

    @Test
    void asksUserInputWhenBarcodeIsUnknown() {
        when(adapter.process("code")).thenReturn(
                new CodeAdapterResult(CodeAdapterOutcome.BARCODE, null, null, "4601"));
        when(catalog.findProductByCode("4601")).thenReturn(Optional.empty());

        ScanResult result = service.scan("code");

        assertThat(result.status()).isEqualTo(ScanStatus.USER_INPUT_REQUIRED);
        assertThat(result.barcode()).isEqualTo("4601");
    }

    @Test
    void failsOnContractViolation() {
        when(adapter.process("code")).thenReturn(new CodeAdapterResult(null, null, null, null));
        assertThatThrownBy(() -> service.scan("code")).isInstanceOf(UpstreamContractException.class);

        when(adapter.process("blank")).thenReturn(
                new CodeAdapterResult(CodeAdapterOutcome.BARCODE, null, null, " "));
        assertThatThrownBy(() -> service.scan("blank")).isInstanceOf(UpstreamContractException.class);
    }

    @Test
    void preparesCzResponseForApp() {
        when(adapter.prepareProductForApp(Map.of("a", 1))).thenReturn(PRODUCT);

        assertThat(service.acceptCzResponse(Map.of("a", 1)).product()).isEqualTo(PRODUCT);
    }
}
