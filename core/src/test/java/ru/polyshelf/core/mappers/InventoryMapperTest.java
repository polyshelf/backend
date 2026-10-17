package ru.polyshelf.core.mappers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.polyshelf.core.dto.InventoryItemResponse;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.models.ProductEntity;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMapperTest {

    private final InventoryMapper mapper = Mappers.getMapper(InventoryMapper.class);

    @Test
    void toProductResponse_shouldMapCustomProduct() {
        UUID itemId = UUID.randomUUID();
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);

        InventoryItemEntity item = new InventoryItemEntity()
                .setId(itemId)
                .setCustomName("Клубника")
                .setExpirationDate(expirationDate);

        ProductResponse response = mapper.toProductResponse(item);

        assertAll(
                () -> assertEquals(itemId, response.id()),
                () -> assertEquals("Клубника", response.name()),
                () -> assertEquals(
                        expirationDate,
                        response.expirationDate()
                )
        );
    }

    @Test
    void toProductResponse_shouldUseCatalogProductName() {
        ProductEntity catalogProduct = new ProductEntity();
        catalogProduct.setName("Молоко Простоквашино");

        InventoryItemEntity item = new InventoryItemEntity()
                .setId(UUID.randomUUID())
                .setProduct(catalogProduct)
                .setExpirationDate(LocalDate.of(2026, Month.NOVEMBER, 1));

        ProductResponse response = mapper.toProductResponse(item);

        assertEquals("Молоко Простоквашино", response.name());
    }

    @Test
    void toProductResponse_shouldPreferCustomName() {
        ProductEntity catalogProduct = new ProductEntity();
        catalogProduct.setName("Название из каталога");

        InventoryItemEntity item = new InventoryItemEntity()
                .setId(UUID.randomUUID())
                .setProduct(catalogProduct)
                .setCustomName("Моё название")
                .setExpirationDate(LocalDate.of(2026, Month.NOVEMBER, 1));

        ProductResponse response = mapper.toProductResponse(item);

        assertEquals("Моё название", response.name());
    }

    @Test
    void toInventoryItemResponse_shouldMapCustomProductAndTimestamps() {
        UUID itemId = UUID.randomUUID();
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);
        OffsetDateTime consumedAt = OffsetDateTime.parse("2026-10-17T11:00:00Z");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-10-16T09:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-17T11:00:00Z");
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(itemId)
                .setCustomName("Клубника")
                .setExpirationDate(expirationDate)
                .setConsumedAt(consumedAt)
                .setCreatedAt(createdAt)
                .setUpdatedAt(updatedAt);

        InventoryItemResponse response = mapper.toInventoryItemResponse(item);

        assertAll(
                () -> assertEquals(itemId, response.id()),
                () -> assertEquals("Клубника", response.name()),
                () -> assertEquals(expirationDate, response.expirationDate()),
                () -> assertEquals(consumedAt, response.consumedAt()),
                () -> assertEquals(createdAt, response.createdAt()),
                () -> assertEquals(updatedAt, response.updatedAt())
        );
    }

    @Test
    void toInventoryItemResponse_shouldUseCatalogProductName() {
        ProductEntity product = new ProductEntity();
        product.setName("Молоко");
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(UUID.randomUUID())
                .setProduct(product)
                .setExpirationDate(LocalDate.of(2026, Month.OCTOBER, 25));

        InventoryItemResponse response = mapper.toInventoryItemResponse(item);

        assertEquals("Молоко", response.name());
        assertNull(response.consumedAt());
    }

    @Test
    void toInventoryItemResponse_shouldPreferCustomName() {
        ProductEntity product = new ProductEntity();
        product.setName("Название из каталога");
        InventoryItemEntity item = new InventoryItemEntity()
                .setCustomName("Моё название")
                .setProduct(product);

        InventoryItemResponse response = mapper.toInventoryItemResponse(item);

        assertEquals("Моё название", response.name());
    }

}
