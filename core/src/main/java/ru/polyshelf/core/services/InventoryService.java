package ru.polyshelf.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithBarcodeRequest;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.mappers.InventoryMapper;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.InventoryRepository;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductService productService;

    @Transactional
    public ProductResponse addWithoutCode(UUID userId, ProductWithoutCodeRequest request) {
        InventoryItemEntity item = new InventoryItemEntity()
                .setUserId(userId)
                .setCustomName(request.name().strip())
                .setExpirationDate(request.expirationDate());

        InventoryItemEntity saved = inventoryRepository.save(item);

        return inventoryMapper.toProductResponse(saved);
    }

    @Transactional
    public ProductResponse addWithCode(UUID userId, ProductWithBarcodeRequest request) {
        ProductEntity product = productService.getProductByCode(request.code().strip());

        InventoryItemEntity item = new InventoryItemEntity()
                .setUserId(userId)
                .setProduct(product)
                .setExpirationDate(request.expirationDate());

        InventoryItemEntity saved = inventoryRepository.save(item);

        return inventoryMapper.toProductResponse(saved);
    }

}
