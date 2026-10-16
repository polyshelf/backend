package ru.polyshelf.core.mappers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.models.ProductEntity;

import java.time.LocalDate;
import java.time.Month;
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
}
