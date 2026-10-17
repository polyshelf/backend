package ru.polyshelf.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.polyshelf.core.models.InventoryItemEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItemEntity, UUID> {

    Optional<InventoryItemEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

}
