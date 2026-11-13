package ru.polyshelf.core.services;

import com.jerolba.carpet.CloseableIterator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.polyshelf.core.config.OpenFoodFactsProperties;
import ru.polyshelf.core.dto.OffProductDto;
import ru.polyshelf.core.io.HttpRetryExecutor;
import ru.polyshelf.core.io.OpenFoodFactsSource;
import ru.polyshelf.core.mappers.ProductMapper;
import ru.polyshelf.core.models.FetchHistory;
import ru.polyshelf.core.models.ProductEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpenFoodFactsImportServiceTest {
    private static final String REVISION = "a".repeat(40);
    private final FetchHistoryService store = mock(FetchHistoryService.class);
    private final OpenFoodFactsSource source = mock(OpenFoodFactsSource.class);
    private final ProductMapper mapper = mock(ProductMapper.class);
    private final OpenFoodFactsProperties properties = new OpenFoodFactsProperties();
    private final ValidatorFactory validation = Validation.buildDefaultValidatorFactory();
    private final AtomicReference<FetchHistory> durable = new AtomicReference<>();
    private OpenFoodFactsImportService service;

    @BeforeEach
    void setUp() throws IOException {
        properties.setBatchSize(2);
        properties.getReader().setRetryDelay(Duration.ZERO);
        service = newService();
        when(store.load("OPENFOODFACTS")).thenAnswer(call -> Optional.ofNullable(durable.get()));
        lenient().when(source.latestRevision()).thenReturn(REVISION);
        lenient().when(store.startPass(anyString(), anyString(), any())).thenAnswer(call -> {
            var checkpoint = checkpoint(call.getArgument(1), 0);
            durable.set(checkpoint);
            return checkpoint;
        });
        lenient().when(store.commitBatch(any(), anyList(), anyLong())).thenAnswer(call -> {
            FetchHistory before = call.getArgument(0);
            assertEquals(durable.get(), before);
            var saved = checkpoint(before.revision(), call.getArgument(2));
            durable.set(saved);
            return saved;
        });
        lenient().when(store.completePass(any(), anyList(), anyString())).thenAnswer(call -> {
            FetchHistory before = call.getArgument(0);
            assertEquals(durable.get(), before);
            var saved = checkpoint(call.getArgument(2), 0);
            durable.set(saved);
            return saved;
        });
        lenient().when(mapper.toProductEntity(any())).thenAnswer(call -> {
            OffProductDto dto = call.getArgument(0);
            return new ProductEntity().setBarcode(dto.code()).setName(dto.code());
        });
    }

    @AfterEach
    void close() throws IOException {
        service.closeReader();
        validation.close();
    }

    @Test
    void sharesIteratorAndAdvancesRawRowsEvenWhenValidationRejectsProducts() throws IOException {
        var rows = products(5);
        when(source.open(REVISION)).thenReturn(rows);
        when(mapper.toProductEntity(new OffProductDto("0", null)))
                .thenReturn(new ProductEntity().setBarcode("0"));

        service.importProducts();
        assertEquals(2, durable.get().nextRow());
        service.importProducts();
        assertEquals(4, durable.get().nextRow());

        var batches = batches(2);
        assertEquals(List.of("1"), batches.getFirst().stream().map(ProductEntity::getBarcode).toList());
        assertEquals(List.of("2", "3"), batches.getLast().stream().map(ProductEntity::getBarcode).toList());
        verify(source).open(REVISION);
        verify(source).latestRevision();
        verify(rows, times(4)).next();
        verify(rows, never()).close();
    }

    @Test
    void restartUsesSavedRevisionAndRawOffset() throws IOException {
        String pinned = "b".repeat(40);
        durable.set(checkpoint(pinned, 2));
        var rows = products(5);
        when(source.open(pinned)).thenReturn(rows);

        service.initializeProgress();
        verify(source, never()).open(anyString());
        service.importProducts();

        assertEquals(List.of("2", "3"), batches(1).getFirst().stream().map(ProductEntity::getBarcode).toList());
        assertEquals(4, durable.get().nextRow());
        assertEquals(pinned, durable.get().revision());
        verify(source, never()).latestRevision();
    }

    @Test
    void retriesTheSamePendingBatchAfterDatabaseFailureWithoutReadingMoreRows() throws IOException {
        var rows = products(5);
        when(source.open(REVISION)).thenReturn(rows);
        doThrow(new IllegalStateException("Database unavailable"))
                .doAnswer(call -> {
                    var saved = checkpoint(REVISION, call.getArgument(2));
                    durable.set(saved);
                    return saved;
                }).when(store).commitBatch(any(), anyList(), anyLong());

        assertThrows(IllegalStateException.class, service::importProducts);
        assertEquals(0, durable.get().nextRow());
        service.importProducts();

        var batches = batches(2);
        assertSame(batches.get(0), batches.get(1));
        verify(rows, times(2)).next();
        verify(source).open(REVISION);
        assertEquals(2, durable.get().nextRow());
    }

    @Test
    void failedParquetReadReopensAtLastCommittedCheckpointAndDiscardsPartialBatch() throws IOException {
        durable.set(checkpoint(REVISION, 2));
        var broken = products(6);
        when(broken.next()).thenReturn(new OffProductDto("0", null), new OffProductDto("1", null),
                        new OffProductDto("2", null))
                .thenThrow(new UncheckedIOException(new IOException("Range failed")));
        var recovered = products(6);
        when(source.open(REVISION)).thenReturn(broken, recovered);

        service.importProducts();

        verify(broken).close();
        verify(source, times(2)).open(REVISION);
        assertEquals(List.of("2", "3"), batches(1).getFirst().stream().map(ProductEntity::getBarcode).toList());
        assertEquals(4, durable.get().nextRow());
    }

    @Test
    void exhaustedRecoveryKeepsDurableOffsetForTheNextScheduledRun() throws IOException {
        durable.set(checkpoint(REVISION, 2));
        when(source.open(REVISION)).thenThrow(new IOException("Offline"));

        assertThrows(IOException.class, service::importProducts);

        verify(source, times(2)).open(REVISION);
        verify(store, never()).commitBatch(any(), anyList(), anyLong());
        assertEquals(2, durable.get().nextRow());
    }

    @Test
    void permanentHttpFailureDoesNotCauseImmediateReaderRetries() throws IOException {
        durable.set(checkpoint(REVISION, 2));
        when(source.open(REVISION)).thenThrow(new HttpRetryExecutor.PermanentHttpException("HTTP 404"));

        assertThrows(IOException.class, service::importProducts);

        verify(source).open(REVISION);
        assertEquals(2, durable.get().nextRow());
    }

    @Test
    void fullFinalBatchCommitsNextRevisionBeforeNextScheduledRun() throws IOException {
        String nextRevision = "b".repeat(40);
        when(source.latestRevision()).thenReturn(REVISION, nextRevision);
        var first = products(2);
        var second = products(3);
        when(source.open(REVISION)).thenReturn(first);
        when(source.open(nextRevision)).thenReturn(second);

        service.importProducts();
        assertEquals(0, durable.get().nextRow());
        assertEquals(nextRevision, durable.get().revision());
        verify(first).close();
        verify(source, times(2)).latestRevision();
        service.importProducts();

        assertEquals(nextRevision, durable.get().revision());
        assertEquals(2, durable.get().nextRow());
        verify(source, times(2)).latestRevision();
    }

    @Test
    void restartAtZeroUsesRevisionAlreadySelectedByPreviousPass() throws IOException {
        String pinned = "b".repeat(40);
        durable.set(checkpoint(pinned, 0));
        var rows = products(3);
        when(source.open(pinned)).thenReturn(rows);

        service.importProducts();

        assertEquals(pinned, durable.get().revision());
        assertEquals(2, durable.get().nextRow());
        verify(source, never()).latestRevision();
    }

    @Test
    void finalBatchDatabaseFailureKeepsOldRevisionAndRetriesTheSameBatchAndSelectedSha() throws IOException {
        String nextRevision = "b".repeat(40);
        durable.set(checkpoint(REVISION, 2));
        when(source.latestRevision()).thenReturn(nextRevision);
        var rows = products(4);
        when(source.open(REVISION)).thenReturn(rows);
        doThrow(new IllegalStateException("Database unavailable"))
                .doAnswer(call -> {
                    var saved = checkpoint(call.getArgument(2), 0);
                    durable.set(saved);
                    return saved;
                }).when(store).completePass(any(), anyList(), anyString());

        assertThrows(IllegalStateException.class, service::importProducts);
        assertEquals(REVISION, durable.get().revision());
        assertEquals(2, durable.get().nextRow());
        service.importProducts();

        var batches = completedBatches(2);
        assertSame(batches.getFirst(), batches.getLast());
        assertEquals(nextRevision, durable.get().revision());
        assertEquals(0, durable.get().nextRow());
        verify(source).latestRevision();
        verify(source).open(REVISION);
        verify(rows, times(4)).next();
    }

    @Test
    void shaLookupFailureRetainsFinalBatchAndCheckpointUntilNextAttempt() throws IOException {
        String nextRevision = "b".repeat(40);
        durable.set(checkpoint(REVISION, 2));
        when(source.latestRevision()).thenThrow(new IOException("Hub unavailable")).thenReturn(nextRevision);
        var rows = products(4);
        when(source.open(REVISION)).thenReturn(rows);

        assertThrows(IOException.class, service::importProducts);
        assertEquals(REVISION, durable.get().revision());
        assertEquals(2, durable.get().nextRow());
        verify(store, never()).completePass(any(), anyList(), anyString());
        service.importProducts();

        assertEquals(nextRevision, durable.get().revision());
        assertEquals(0, durable.get().nextRow());
        assertEquals(List.of("2", "3"), completedBatches(1).getFirst().stream()
                .map(ProductEntity::getBarcode).toList());
        verify(source).open(REVISION);
        verify(rows, times(4)).next();
    }

    @Test
    void invalidOnlyBatchStillCommitsProgressAndAllRequiredFieldsAreValidated() throws IOException {
        properties.setBatchSize(7);
        var rows = products(8);
        when(source.open(REVISION)).thenReturn(rows);
        doReturn(
                new ProductEntity().setName("Name"),
                new ProductEntity().setBarcode("Code"),
                new ProductEntity().setBarcode("x".repeat(33)).setName("Name"),
                new ProductEntity().setBarcode("Code").setName("x".repeat(256)),
                new ProductEntity().setBarcode(" ").setName("Name"),
                new ProductEntity().setBarcode("Code").setName(" "),
                new ProductEntity().setBarcode("Code").setName("N\0ame"))
                .when(mapper).toProductEntity(any());

        service.importProducts();

        assertTrue(batches(1).getFirst().isEmpty());
        assertEquals(7, durable.get().nextRow());
    }

    @Test
    void shutdownClosesTheOpenIterator() throws IOException {
        var rows = products(5);
        when(source.open(REVISION)).thenReturn(rows);
        service.importProducts();

        service.closeReader();

        verify(rows).close();
    }

    private OpenFoodFactsImportService newService() {
        return new OpenFoodFactsImportService(store, source, mapper, validation.getValidator(), properties);
    }

    private FetchHistory checkpoint(String revision, long row) {
        return new FetchHistory("OPENFOODFACTS", revision, row, OffsetDateTime.now());
    }

    private CloseableIterator<OffProductDto> products(int count) {
        var values = IntStream.range(0, count).mapToObj(i -> new OffProductDto(Integer.toString(i), null)).iterator();
        @SuppressWarnings("unchecked")
        CloseableIterator<OffProductDto> iterator = mock(CloseableIterator.class);
        lenient().when(iterator.hasNext()).thenAnswer(call -> values.hasNext());
        lenient().when(iterator.next()).thenAnswer(call -> values.next());
        return iterator;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<List<ProductEntity>> batches(int calls) {
        ArgumentCaptor<List<ProductEntity>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(store, times(calls)).commitBatch(any(), captor.capture(), anyLong());
        return captor.getAllValues();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<List<ProductEntity>> completedBatches(int calls) {
        ArgumentCaptor<List<ProductEntity>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(store, times(calls)).completePass(any(), captor.capture(), anyString());
        return captor.getAllValues();
    }
}
