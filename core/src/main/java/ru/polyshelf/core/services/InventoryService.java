package ru.polyshelf.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithBarcodeCandidateRequest;
import ru.polyshelf.core.dto.ProductWithBarcodeRequest;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.mappers.InventoryMapper;
import ru.polyshelf.core.models.InventoryItemEntity;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.InventoryRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductService productService;
    private final ProductCandidateService productCandidateService;

    @Transactional
    public ProductResponse addWithoutCode(UUID userId, ProductWithoutCodeRequest request) {
        return saveCustom(userId, request.name(), request.expirationDate());
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

    @Transactional
    public ProductResponse addWithCodeCandidate(UUID userId, ProductWithBarcodeCandidateRequest request) {
        productService.checkNotExistsInCatalog(request.code());

        productCandidateService.addByUser(userId, request.code(), request.name());

        return saveCustom(userId, request.name(), request.expirationDate());
    }

    @Transactional
    public void delete(UUID userId, UUID itemId) {
        InventoryItemEntity item = inventoryRepository
                .findByIdAndUserIdAndDeletedAtIsNull(itemId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        item.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void setConsumed(UUID userId, UUID itemId, boolean isConsumed) {
        InventoryItemEntity item = inventoryRepository
                .findByIdAndUserIdAndDeletedAtIsNull(itemId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean alreadyConsumed = item.getConsumedAt() != null;

        if (alreadyConsumed == isConsumed) {
            return;
        }

        item.setConsumedAt(isConsumed ? OffsetDateTime.now(ZoneOffset.UTC) : null);
    }

    private ProductResponse saveCustom(UUID userId, String name, LocalDate expirationDate) {
        InventoryItemEntity item = new InventoryItemEntity()
                .setUserId(userId)
                .setCustomName(name.strip())
                .setExpirationDate(expirationDate);

        InventoryItemEntity saved = inventoryRepository.save(item);

        return inventoryMapper.toProductResponse(saved);
    }
}
