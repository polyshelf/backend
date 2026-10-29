package ru.polyshelf.core.repositories;

import ru.polyshelf.core.models.ProductEntity;

import java.util.List;
import java.util.OptionalInt;

public interface ProductRepositoryCustom {
    OptionalInt upsertBatchFromOffIfAbsent(List<ProductEntity> entities, String offSource);
}
