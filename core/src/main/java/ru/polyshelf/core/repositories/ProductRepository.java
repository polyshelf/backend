package ru.polyshelf.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.polyshelf.core.models.ProductEntity;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

    Optional<ProductEntity> findByBarcode(String barcode);

}
