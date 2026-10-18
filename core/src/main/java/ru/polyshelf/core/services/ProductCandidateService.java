package ru.polyshelf.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.polyshelf.core.models.ProductCandidateEntity;
import ru.polyshelf.core.repositories.ProductCandidateRepository;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductCandidateService {

    private final ProductCandidateRepository productCandidateRepository;

    @Transactional
    protected void addByUser(UUID userId, String code, String name) {
        ProductCandidateEntity candidate = new ProductCandidateEntity()
                .setBarcode(code.strip())
                .setName(name.strip())
                .setUserId(userId);

        productCandidateRepository.save(candidate);
    }

}
