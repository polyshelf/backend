package ru.polyshelf.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.polyshelf.core.models.FetchHistory;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.FetchHistoryRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FetchHistoryService {
    private final FetchHistoryRepository repository;
    private final ProductService productService;

    public Optional<FetchHistory> load(String source) {
        return repository.find(source);
    }

    @Transactional
    public FetchHistory startPass(String source, String revision, FetchHistory expected) {
        if (expected == null) {
            repository.insertIfAbsent(source, revision);
            return repository.lock(source);
        }
        assertCurrent(expected);
        if (expected.nextRow() != 0) {
            throw new IllegalStateException("Cannot replace an unfinished dataset revision");
        }
        return expected.revision().equals(revision) ? expected : repository.update(source, revision, 0);
    }

    @Transactional
    public FetchHistory commitBatch(FetchHistory expected, List<ProductEntity> products, long nextRow) {
        assertCurrent(expected);
        if (!products.isEmpty()) {
            productService.upsertFromOffIfAbsent(products, expected.source());
        }
        return repository.update(expected.source(), expected.revision(), nextRow);
    }

    private void assertCurrent(FetchHistory expected) {
        if (!expected.equals(repository.lock(expected.source()))) {
            throw new OptimisticLockingFailureException("Import progress was changed by another execution");
        }
    }
}
