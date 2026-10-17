package ru.polyshelf.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.mappers.InventoryMapper;
import ru.polyshelf.core.models.InventoryItemEntity;
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
}
