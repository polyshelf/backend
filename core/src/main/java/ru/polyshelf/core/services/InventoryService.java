package ru.polyshelf.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.mappers.InventoryMapper;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.repositories.InventoryRepository;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public ProductResponse addWithoutCode(UUID userId, ProductWithoutCodeRequest request) {
        InventoryItemEntity item = new InventoryItemEntity();
        item.setUserId(userId)
                .setCustomName(request.name().strip())
                .setExpirationDate(request.expirationDate());

        InventoryItemEntity saved = inventoryRepository.save(item);

        return inventoryMapper.toProductResponse(saved);
    }

}
