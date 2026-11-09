package ru.polyshelf.core.services;

import com.jerolba.carpet.CloseableIterator;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.polyshelf.core.config.OpenFoodFactsProperties;
import ru.polyshelf.core.dto.OffProductDto;
import ru.polyshelf.core.io.HttpRetryExecutor;
import ru.polyshelf.core.io.OpenFoodFactsSource;
import ru.polyshelf.core.mappers.ProductMapper;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.models.FetchHistory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenFoodFactsImportService {
    private static final String SOURCE = "OPENFOODFACTS";
    private final FetchHistoryService historyService;
    private final OpenFoodFactsSource source;
    private final ProductMapper productMapper;
    private final Validator validator;
    private final OpenFoodFactsProperties properties;

    private FetchHistory history;
    private CloseableIterator<OffProductDto> reader;
    private long readerNextRow;
    private Batch pendingBatch;
    private boolean initialized;
    private boolean selectRevision;

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void initializeProgress() {
        if (!initialized) {
            history = historyService.load(SOURCE).orElse(null);
            selectRevision = history == null || history.nextRow() == 0;
            initialized = true;
            log.info("Loaded OFF progress: revision={}, nextRow={}",
                    history == null ? null : history.revision(), history == null ? 0 : history.nextRow());
        }
    }

    @Scheduled(cron = "${openfoodfacts.cron:0 */20 * * * *}")
    public synchronized void importProducts() throws IOException {
        initializeProgress();
        try {
            if (pendingBatch == null) {
                pendingBatch = readWithRecovery();
            }
            // Keep the batch on database failure instead of advancing the iterator again.
            history = historyService.commitBatch(history, pendingBatch.products(), pendingBatch.nextRow());
            boolean completed = pendingBatch.completed();
            log.info("Committed OFF batch: products={}, nextRow={}, completed={}",
                    pendingBatch.products().size(), history.nextRow(), completed);
            pendingBatch = null;
            if (completed) {
                selectRevision = true;
                closeReader();
            }
        } catch (OptimisticLockingFailureException e) {
            // Another process (or an uncertain previous commit) moved the checkpoint.
            pendingBatch = null;
            initialized = false;
            discardReader(e);
            throw e;
        }
    }

    private Batch readWithRecovery() throws IOException {
        int maxAttempts = properties.getReader().getMaxAttempts();
        for (int attempt = 1; ; attempt++) {
            try {
                openReaderIfNeeded();
                var batch = new ArrayList<ProductEntity>();
                int batchSize = properties.getBatchSize();
                int readCount = 0;
                while (readCount < batchSize && reader.hasNext()) {
                    var dto = reader.next();
                    readerNextRow++;
                    readCount++;
                    var product = dto == null ? null : productMapper.toProductEntity(dto);
                    if (isValid(product)) {
                        batch.add(product);
                    }
                }
                boolean completed = !reader.hasNext();
                // Zero marks the next pass; rejected raw rows still advance progress.
                return new Batch(List.copyOf(batch), completed ? 0 : readerNextRow, completed);
            } catch (IOException | RuntimeException e) {
                discardReader(e);
                if (attempt >= maxAttempts || Thread.currentThread().isInterrupted() || isPermanent(e)) {
                    throw e;
                }
                log.warn("OFF reader attempt {}/{} failed; reopening revision={} at row={}",
                        attempt, maxAttempts, history == null ? null : history.revision(),
                        history == null ? 0 : history.nextRow(), e);
                pauseBeforeRecovery();
            }
        }
    }

    private void openReaderIfNeeded() throws IOException {
        if (reader != null) {
            return;
        }
        if (selectRevision) {
            history = historyService.startPass(SOURCE, source.latestRevision(), history);
            selectRevision = false;
        }
        reader = source.open(history.revision());
        readerNextRow = 0;
        // Only a restart/recovery replays raw rows. Scheduled calls share the iterator.
        while (readerNextRow < history.nextRow()) {
            if (!reader.hasNext()) {
                throw new HttpRetryExecutor.PermanentHttpException("Saved OFF position exceeds pinned dataset size");
            }
            reader.next();
            readerNextRow++;
        }
    }

    private boolean isValid(ProductEntity product) {
        if (product == null) {
            return false;
        }
        product.setSource(ProductEntity.Source.OPENFOODFACTS);
        return StringUtils.hasText(product.getBarcode()) && StringUtils.hasText(product.getName())
                && product.getBarcode().indexOf('\0') < 0 && product.getName().indexOf('\0') < 0
                && validator.validate(product).isEmpty();
    }

    private void pauseBeforeRecovery() throws InterruptedIOException {
        try {
            Thread.sleep(properties.getReader().getRetryDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            var failure = new InterruptedIOException("OFF recovery interrupted");
            failure.initCause(e);
            throw failure;
        }
    }

    private static boolean isPermanent(Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpRetryExecutor.PermanentHttpException
                    || cause instanceof OptimisticLockingFailureException) {
                return true;
            }
        }
        return false;
    }

    private void discardReader(Throwable failure) {
        try {
            closeReader();
        } catch (IOException | RuntimeException e) {
            failure.addSuppressed(e);
        }
    }

    @PreDestroy
    synchronized void closeReader() throws IOException {
        var current = reader;
        reader = null;
        if (current != null) {
            current.close();
        }
    }

    private record Batch(List<ProductEntity> products, long nextRow, boolean completed) {
    }
}
