package ru.polyshelf.core.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.polyshelf.core.models.ProductEntity;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
    private final JdbcTemplate jdbc;

    @Override
    public OptionalInt upsertBatchFromOffIfAbsent(List<ProductEntity> entities, String offSource) {
        return Arrays.stream(
                jdbc.batchUpdate(
                        """
                            INSERT INTO product_in_catalog (barcode, name, source)
                            VALUES (?, ?, '%s')
                            ON CONFLICT (barcode)
                            DO UPDATE SET
                                name = EXCLUDED.name,
                                updated_at = NOW()
                            WHERE product_in_catalog.source = '%s'
                        """.formatted(offSource, offSource),
                        entities
                                .parallelStream()
                                .filter(entity -> entity.getName() != null)
                                .map(entity ->
                                        new Object[]{
                                                entity.getBarcode(),
                                                entity.getName()
                                        }
                                )
                                .toList()
                )).reduce(Integer::sum);
    }
}
