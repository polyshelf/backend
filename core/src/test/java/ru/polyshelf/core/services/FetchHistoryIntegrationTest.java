package ru.polyshelf.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.polyshelf.core.TestcontainersConfiguration;
import ru.polyshelf.core.models.ProductEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "openfoodfacts.cron=-")
@Import(TestcontainersConfiguration.class)
class FetchHistoryIntegrationTest {
    private static final String SOURCE = "OPENFOODFACTS";
    private static final String REVISION = "a".repeat(40);
    @Autowired
    private FetchHistoryService history;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM fetch_history WHERE source = ?", SOURCE);
        jdbc.update("DELETE FROM product_in_catalog WHERE barcode LIKE 'fetch-test-%'");
    }

    @Test
    void productAndCheckpointCommitTogetherAndExistingOffProductsAreUpdated() {
        var before = history.startPass(SOURCE, REVISION, null);
        var product = new ProductEntity().setBarcode("fetch-test-good").setName("First");

        var saved = history.commitBatch(before, List.of(product), 10000);
        assertEquals(10000, history.load(SOURCE).orElseThrow().nextRow());
        assertEquals(REVISION, saved.revision());

        history.commitBatch(saved, List.of(product.setName("Updated")), 20000);
        assertEquals("Updated", jdbc.queryForObject(
                "SELECT name FROM product_in_catalog WHERE barcode = ?", String.class, product.getBarcode()));
    }

    @Test
    void failedProductBatchRollsBackProductsAndCheckpoint() {
        var before = history.startPass(SOURCE, REVISION, null);
        var valid = new ProductEntity().setBarcode("fetch-test-rollback").setName("Valid");
        var invalid = new ProductEntity().setName("Missing barcode");

        assertThrows(DataAccessException.class, () -> history.commitBatch(before, List.of(valid, invalid), 10000));

        assertEquals(before, history.load(SOURCE).orElseThrow());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM product_in_catalog WHERE barcode = ?",
                Integer.class, valid.getBarcode()));
    }

    @Test
    void staleCheckpointCannotWriteProductsOrOverwriteProgress() {
        var before = history.startPass(SOURCE, REVISION, null);
        history.commitBatch(before, List.of(), 10000);

        assertThrows(OptimisticLockingFailureException.class, () -> history.commitBatch(before,
                List.of(new ProductEntity().setBarcode("fetch-test-stale").setName("Stale")), 20000));

        assertEquals(10000, history.load(SOURCE).orElseThrow().nextRow());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM product_in_catalog WHERE barcode = ?",
                Integer.class, "fetch-test-stale"));
    }

    @Test
    void unfinishedPassCannotSwitchRevisionAndEmptyBatchCanAdvanceProgress() {
        var before = history.startPass(SOURCE, REVISION, null);
        var saved = history.commitBatch(before, List.of(), 10000);

        assertThrows(IllegalStateException.class, () -> history.startPass(SOURCE, "b".repeat(40), saved));

        assertEquals(REVISION, history.load(SOURCE).orElseThrow().revision());
        assertEquals(10000, history.load(SOURCE).orElseThrow().nextRow());
    }
}
