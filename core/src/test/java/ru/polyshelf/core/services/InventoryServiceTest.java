package ru.polyshelf.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.dto.InventoryItemResponse;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithBarcodeRequest;
import ru.polyshelf.core.dto.ProductWithBarcodeCandidateRequest;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.mappers.InventoryMapper;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.InventoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.Optional;
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

    @Mock
    private ProductCandidateService productCandidateService;

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

    @Test
    void addWithCodeCandidate_shouldSaveCandidateAndCustomInventoryItem() {
        String barcode = "4601234567890";
        LocalDate expirationDate = LocalDate.of(2026, Month.OCTOBER, 20);
        ProductWithBarcodeCandidateRequest request = new ProductWithBarcodeCandidateRequest(
                barcode, "  Молоко  ", expirationDate
        );
        ProductResponse expectedResponse = new ProductResponse(ITEM_ID, "Молоко", expirationDate);

        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenAnswer(invocation -> {
                    InventoryItemEntity item = invocation.getArgument(0);
                    return item.setId(ITEM_ID);
                });
        when(inventoryMapper.toProductResponse(any(InventoryItemEntity.class)))
                .thenReturn(expectedResponse);

        ProductResponse result = inventoryService.addWithCodeCandidate(USER_ID, request);

        ArgumentCaptor<InventoryItemEntity> captor =
                ArgumentCaptor.forClass(InventoryItemEntity.class);
        verify(productService).checkNotExistsInCatalog(barcode);
        verify(productCandidateService).addByUser(USER_ID, barcode, "  Молоко  ");
        verify(inventoryRepository).save(captor.capture());
        InventoryItemEntity item = captor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, item.getUserId()),
                () -> assertEquals("Молоко", item.getCustomName()),
                () -> assertNull(item.getProduct()),
                () -> assertEquals(expirationDate, item.getExpirationDate()),
                () -> assertEquals(ITEM_ID, item.getId()),
                () -> assertEquals(expectedResponse, result)
        );
        verify(inventoryMapper).toProductResponse(item);
    }

    @Test
    void addWithCodeCandidate_shouldNotSaveWhenProductAlreadyExists() {
        ProductWithBarcodeCandidateRequest request = new ProductWithBarcodeCandidateRequest(
                "4601234567890", "Молоко", LocalDate.of(2026, Month.OCTOBER, 20)
        );
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Product already exists in catalog"))
                .when(productService).checkNotExistsInCatalog(request.code());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.addWithCodeCandidate(USER_ID, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(productService).checkNotExistsInCatalog(request.code());
        verifyNoInteractions(productCandidateService, inventoryRepository, inventoryMapper);
    }

    @Test
    void addWithCodeCandidate_shouldNotSaveInventoryWhenCandidateSaveFails() {
        ProductWithBarcodeCandidateRequest request = new ProductWithBarcodeCandidateRequest(
                "4601234567890", "Молоко", LocalDate.of(2026, Month.OCTOBER, 20)
        );
        doThrow(new RuntimeException("Candidate save failed"))
                .when(productCandidateService)
                .addByUser(USER_ID, request.code(), request.name());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.addWithCodeCandidate(USER_ID, request)
        );

        assertEquals("Candidate save failed", exception.getMessage());
        verify(productService).checkNotExistsInCatalog(request.code());
        verify(productCandidateService).addByUser(USER_ID, request.code(), request.name());
        verifyNoInteractions(inventoryRepository, inventoryMapper);
    }

    @Test
    void addWithCodeCandidate_shouldPropagateInventorySaveException() {
        ProductWithBarcodeCandidateRequest request = new ProductWithBarcodeCandidateRequest(
                "4601234567890", "Молоко", LocalDate.of(2026, Month.OCTOBER, 20)
        );
        when(inventoryRepository.save(any(InventoryItemEntity.class)))
                .thenThrow(new RuntimeException("Inventory save failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.addWithCodeCandidate(USER_ID, request)
        );

        assertEquals("Inventory save failed", exception.getMessage());
        verify(productService).checkNotExistsInCatalog(request.code());
        verify(productCandidateService).addByUser(USER_ID, request.code(), request.name());
        verify(inventoryRepository).save(any(InventoryItemEntity.class));
        verifyNoInteractions(inventoryMapper);
    }

    @Test
    void delete_shouldSetDeletedAtForOwnedItem() {
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(ITEM_ID)
                .setUserId(USER_ID);

        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.of(item));

        OffsetDateTime before = OffsetDateTime.now();
        inventoryService.delete(USER_ID, ITEM_ID);
        OffsetDateTime after = OffsetDateTime.now();

        assertNotNull(item.getDeletedAt());
        assertFalse(item.getDeletedAt().isBefore(before));
        assertFalse(item.getDeletedAt().isAfter(after));

        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
        verifyNoInteractions(inventoryMapper, productService, productCandidateService);
    }

    @Test
    void delete_shouldReturn404WhenItemDoesNotExist() {
        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.delete(USER_ID, ITEM_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verifyNoMoreInteractions(inventoryRepository);
        verifyNoInteractions(inventoryMapper, productService, productCandidateService);
    }

    @Test
    void delete_shouldReturn404ForAnotherUsersItem() {
        UUID otherUserId = UUID.randomUUID();

        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, otherUserId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.delete(otherUserId, ITEM_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, otherUserId);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void delete_shouldReturn404ForAlreadyDeletedItem() {
        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.delete(USER_ID, ITEM_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void setConsumed_shouldMarkItemConsumed() {
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(ITEM_ID)
                .setUserId(USER_ID);

        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.of(item));

        OffsetDateTime before = OffsetDateTime.now();
        inventoryService.setConsumed(USER_ID, ITEM_ID, true);
        OffsetDateTime after = OffsetDateTime.now();

        assertNotNull(item.getConsumedAt());
        assertFalse(item.getConsumedAt().isBefore(before));
        assertFalse(item.getConsumedAt().isAfter(after));
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void setConsumed_shouldKeepOriginalTimestampWhenAlreadyConsumed() {
        OffsetDateTime originalConsumedAt = OffsetDateTime.parse("2026-10-16T12:00:00Z");
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(ITEM_ID)
                .setUserId(USER_ID)
                .setConsumedAt(originalConsumedAt);

        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.of(item));

        inventoryService.setConsumed(USER_ID, ITEM_ID, true);

        assertEquals(originalConsumedAt, item.getConsumedAt());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void setConsumed_shouldClearConsumedAtWhenMarkingNotConsumed() {
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(ITEM_ID)
                .setUserId(USER_ID)
                .setConsumedAt(OffsetDateTime.parse("2026-10-16T12:00:00Z"));

        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.of(item));

        inventoryService.setConsumed(USER_ID, ITEM_ID, false);

        assertNull(item.getConsumedAt());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void setConsumed_shouldDoNothingWhenAlreadyNotConsumed() {
        InventoryItemEntity item = new InventoryItemEntity()
                .setId(ITEM_ID)
                .setUserId(USER_ID);

        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.of(item));

        inventoryService.setConsumed(USER_ID, ITEM_ID, false);

        assertNull(item.getConsumedAt());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void setConsumed_shouldReturn404WhenItemIsNotFound() {
        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.setConsumed(USER_ID, ITEM_ID, true)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, USER_ID);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void setConsumed_shouldReturn404WhenItemIsNotOwned() {
        UUID otherUserId = UUID.randomUUID();
        when(inventoryRepository.findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, otherUserId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> inventoryService.setConsumed(otherUserId, ITEM_ID, false)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(inventoryRepository).findByIdAndUserIdAndDeletedAtIsNull(ITEM_ID, otherUserId);
        verify(inventoryRepository, never()).save(any(InventoryItemEntity.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void getInventory_shouldMapItemsAndPassUserIdAndSort() {
        Sort sort = Sort.by(Sort.Direction.ASC, "expirationDate");
        InventoryItemEntity first = new InventoryItemEntity()
                .setId(ITEM_ID)
                .setUserId(USER_ID)
                .setCustomName("Клубника");
        ProductEntity product = new ProductEntity();
        product.setName("Молоко");
        InventoryItemEntity second = new InventoryItemEntity()
                .setId(UUID.randomUUID())
                .setUserId(USER_ID)
                .setProduct(product);
        InventoryItemResponse firstResponse = new InventoryItemResponse(
                ITEM_ID, "Клубника", LocalDate.of(2026, Month.OCTOBER, 20),
                null, null, null
        );
        InventoryItemResponse secondResponse = new InventoryItemResponse(
                second.getId(), "Молоко", LocalDate.of(2026, Month.OCTOBER, 25),
                null, null, null
        );

        when(inventoryRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, sort))
                .thenReturn(List.of(first, second));
        when(inventoryMapper.toInventoryItemResponse(first)).thenReturn(firstResponse);
        when(inventoryMapper.toInventoryItemResponse(second)).thenReturn(secondResponse);

        List<InventoryItemResponse> result = inventoryService.getInventory(USER_ID, sort);

        assertEquals(List.of(firstResponse, secondResponse), result);
        verify(inventoryRepository).findAllByUserIdAndDeletedAtIsNull(USER_ID, sort);
        verify(inventoryMapper).toInventoryItemResponse(first);
        verify(inventoryMapper).toInventoryItemResponse(second);
        verifyNoMoreInteractions(inventoryRepository, inventoryMapper);
        verifyNoInteractions(productService, productCandidateService);
    }

    @Test
    void getInventory_shouldReturnEmptyList() {
        Sort sort = Sort.unsorted();
        when(inventoryRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, sort))
                .thenReturn(List.of());

        List<InventoryItemResponse> result = inventoryService.getInventory(USER_ID, sort);

        assertTrue(result.isEmpty());
        verify(inventoryRepository).findAllByUserIdAndDeletedAtIsNull(USER_ID, sort);
        verifyNoInteractions(inventoryMapper);
    }

    @Test
    void getInventory_shouldPropagateRepositoryException() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        when(inventoryRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, sort))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryService.getInventory(USER_ID, sort));

        assertEquals("Database unavailable", exception.getMessage());
        verifyNoInteractions(inventoryMapper);
    }

}
