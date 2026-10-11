package ru.polyshelf.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.polyshelf.gateway.catalog.client.CatalogClient;
import ru.polyshelf.gateway.catalog.client.NewProductPayload;
import ru.polyshelf.gateway.catalog.exception.InvalidIdempotencyKeyException;
import ru.polyshelf.gateway.catalog.service.CatalogService;
import ru.polyshelf.gateway.catalog.web.NewProductRequest;

class CatalogServiceTest {

    private final CatalogClient client = mock(CatalogClient.class);
    private final CatalogService service = new CatalogService(client);
    private final LocalDate date = LocalDate.of(2030, 1, 1);

    @Test
    void trimsFieldsAndForwardsIdempotencyKey() {
        service.registerNewProduct(5, new NewProductRequest(" 4601 ", " Milk ", date), "key-1");

        ArgumentCaptor<NewProductPayload> payload = ArgumentCaptor.forClass(NewProductPayload.class);
        verify(client).createProduct(payload.capture(), eq("key-1"));
        assertThat(payload.getValue()).isEqualTo(new NewProductPayload(5, "4601", "Milk", date));
    }

    @Test
    void allowsMissingBarcodeAndKey() {
        service.registerNewProduct(5, new NewProductRequest("  ", "Bread", date), null);

        ArgumentCaptor<NewProductPayload> payload = ArgumentCaptor.forClass(NewProductPayload.class);
        verify(client).createProduct(payload.capture(), eq(null));
        assertThat(payload.getValue().barcode()).isNull();
    }

    @Test
    void rejectsMalformedIdempotencyKey() {
        assertThatThrownBy(() -> service.registerNewProduct(5, new NewProductRequest("1", "x", date), "bad key\n"))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
        verify(client, never()).createProduct(any(), any());
    }
}
