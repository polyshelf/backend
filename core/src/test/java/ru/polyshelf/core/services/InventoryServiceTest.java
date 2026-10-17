package ru.polyshelf.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithBarcodeRequest;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.mappers.InventoryMapper;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.InventoryRepository;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private InventoryService inventoryService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Test
    void addWithoutCode_shouldSaveItemAndReturnResponse() {
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);

        ProductWithoutCodeRequest request =
                new ProductWithoutCodeRequest(
                        "  Клубника  ",
                        expirationDate
                );

        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenAnswer(invocation -> {
                    InventoryItemEntity item = invocation.getArgument(0);
                    item.setId(ITEM_ID);
                    return item;
                });

        ProductResponse expectedResponse =
                new ProductResponse(
                        ITEM_ID,
                        "Клубника",
                        expirationDate
                );

        when(inventoryMapper.toProductResponse(
                any(InventoryItemEntity.class)
        )).thenReturn(expectedResponse);

        ProductResponse response =
                inventoryService.addWithoutCode(USER_ID, request);

        ArgumentCaptor<InventoryItemEntity> captor =
                ArgumentCaptor.forClass(InventoryItemEntity.class);

        verify(inventoryRepository).save(captor.capture());

        InventoryItemEntity savedItem = captor.getValue();

        assertAll(
                () -> assertEquals(USER_ID, savedItem.getUserId()),
                () -> assertEquals("Клубника", savedItem.getCustomName()),
                () -> assertEquals(
                        expirationDate,
                        savedItem.getExpirationDate()
                ),
                () -> assertNull(savedItem.getProduct()),
                () -> assertNull(savedItem.getConsumedAt()),
                () -> assertNull(savedItem.getDeletedAt()),
                () -> assertEquals(ITEM_ID, savedItem.getId())
        );

        assertEquals(expectedResponse, response);

        verify(inventoryMapper).toProductResponse(savedItem);
        verifyNoMoreInteractions(inventoryRepository, inventoryMapper);
    }

    @Test
    void addWithoutCode_shouldPropagateRepositoryException() {
        ProductWithoutCodeRequest request =
                new ProductWithoutCodeRequest(
                        "Клубника",
                        LocalDate.of(2026, Month.OCTOBER, 20)
                );

        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.addWithoutCode(USER_ID, request)
        );

        assertEquals("Database unavailable", exception.getMessage());

        verify(inventoryRepository).save(any(InventoryItemEntity.class));
        verifyNoInteractions(inventoryMapper);
    }


    @Test
    void addWithCode_shouldSaveCatalogProductAndReturnResponse() {
        String barcode = "4601234567890";
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);

        ProductWithBarcodeRequest request =
                new ProductWithBarcodeRequest(barcode, expirationDate);

        ProductEntity catalogProduct = new ProductEntity();
        catalogProduct.setName("Молоко");

        when(productService.getProductByCode(barcode))
                .thenReturn(catalogProduct);

        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenAnswer(invocation -> {
                    InventoryItemEntity item = invocation.getArgument(0);
                    item.setId(ITEM_ID);
                    return item;
                });

        ProductResponse expectedResponse =
                new ProductResponse(ITEM_ID, "Молоко", expirationDate);

        when(inventoryMapper.toProductResponse(
                any(InventoryItemEntity.class)
        )).thenReturn(expectedResponse);

        ProductResponse response =
                inventoryService.addWithCode(USER_ID, request);

        ArgumentCaptor<InventoryItemEntity> captor =
                ArgumentCaptor.forClass(InventoryItemEntity.class);

        verify(inventoryRepository).save(captor.capture());

        InventoryItemEntity savedItem = captor.getValue();

        assertAll(
                () -> assertEquals(USER_ID, savedItem.getUserId()),
                () -> assertSame(catalogProduct, savedItem.getProduct()),
                () -> assertNull(savedItem.getCustomName()),
                () -> assertEquals(
                        expirationDate,
                        savedItem.getExpirationDate()
                ),
                () -> assertNull(savedItem.getConsumedAt()),
                () -> assertNull(savedItem.getDeletedAt()),
                () -> assertEquals(ITEM_ID, savedItem.getId())
        );

        assertEquals(expectedResponse, response);

        verify(productService).getProductByCode(barcode);
        verify(inventoryMapper).toProductResponse(savedItem);

        verifyNoMoreInteractions(
                productService,
                inventoryRepository,
                inventoryMapper
        );
    }

    @Test
    void addWithCode_shouldStripBarcodeBeforeSearching() {
        String barcode = "4601234567890";

        ProductWithBarcodeRequest request =
                new ProductWithBarcodeRequest(
                        "  " + barcode + "  ",
                        LocalDate.of(2026, Month.OCTOBER, 20)
                );

        ProductEntity catalogProduct = new ProductEntity();

        when(productService.getProductByCode(barcode))
                .thenReturn(catalogProduct);

        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.addWithCode(USER_ID, request);

        verify(productService).getProductByCode(barcode);
        verify(inventoryRepository).save(any(InventoryItemEntity.class));
    }

    @Test
    void addWithCode_shouldNotSaveWhenProductNotFound() {
        String barcode = "4601234567890";

        ProductWithBarcodeRequest request =
                new ProductWithBarcodeRequest(
                        barcode,
                        LocalDate.of(2026, Month.OCTOBER, 20)
                );

        when(productService.getProductByCode(barcode))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found in catalog"
                        )
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.addWithCode(USER_ID, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productService).getProductByCode(barcode);
        verifyNoInteractions(inventoryRepository, inventoryMapper);
    }

    @Test
    void addWithCode_shouldPropagateRepositoryException() {
        String barcode = "4601234567890";

        ProductWithBarcodeRequest request =
                new ProductWithBarcodeRequest(
                        barcode,
                        LocalDate.of(2026, Month.OCTOBER, 20)
                );

        ProductEntity catalogProduct = new ProductEntity();

        when(productService.getProductByCode(barcode))
                .thenReturn(catalogProduct);

        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.addWithCode(USER_ID, request)
        );

        assertEquals("Database unavailable", exception.getMessage());

        verify(productService).getProductByCode(barcode);
        verify(inventoryRepository).save(any(InventoryItemEntity.class));
        verifyNoInteractions(inventoryMapper);
    }

}
