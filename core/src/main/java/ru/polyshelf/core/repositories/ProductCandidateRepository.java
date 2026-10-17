package ru.polyshelf.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.polyshelf.core.models.ProductCandidateEntity;

@Repository
public interface ProductCandidateRepository extends JpaRepository<ProductCandidateEntity, Integer> {
}
