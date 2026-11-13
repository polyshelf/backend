package ru.polyshelf.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.polyshelf.core.models.ProductEntity;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Integer>, ProductRepositoryCustom {

    Optional<ProductEntity> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

}
