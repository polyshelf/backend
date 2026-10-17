package ru.polyshelf.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.ProductRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    protected ProductEntity getProductByCode(String code) {
        return productRepository.findByBarcode(code.strip())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in catalog"));
    }

}
