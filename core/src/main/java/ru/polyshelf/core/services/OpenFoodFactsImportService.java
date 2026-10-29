package ru.polyshelf.core.services;

import com.jerolba.carpet.CarpetReader;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.polyshelf.core.dto.OffProductDto;
import ru.polyshelf.core.io.HttpInputFile;
import ru.polyshelf.core.mappers.ProductMapper;
import ru.polyshelf.core.models.ProductEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenFoodFactsImportService {
    private final ProductService productService;
    private static final String OFF_SOURCE_NAME = "OPENFOODFACTS";
    private static final String SOURCE_URL = "https://huggingface.co/datasets/openfoodfacts/product-database/resolve/main/food.parquet";
    private final ProductMapper productMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    int offset = 0;

    @Scheduled(initialDelay = 0, fixedDelayString = "${openfoodfacts.retry.delay:PT1M}")
    public void importProducts() throws IOException {
        var inputFile = new HttpInputFile(
                httpClient,
                URI.create(SOURCE_URL)
        );

        List<ProductEntity> batch = new ArrayList<>();
        int batchSize = 10000;
        var reader = new CarpetReader<>(inputFile, OffProductDto.class);
        try (var products = reader.stream()) {
            var iterator = products
                    .skip(offset)
                    .map(productMapper::toProductEntity)
                    .filter(product -> product.getName() != null)
                    .iterator();

            while (iterator.hasNext() && batch.size() < batchSize)
                batch.add(iterator.next());

            // Сохраняем пачку
            if (!batch.isEmpty()) {
                log.info("upserting products batch");
                int upserted = productService.upsertFromOffIfAbsent(batch, OFF_SOURCE_NAME);

                log.info("Upserted {} rows", upserted);

                batch.clear();
                offset += batchSize;
            }

            // Больше нет продуктов
            if (!iterator.hasNext())
                offset = 0;
        }
    }

    @PreDestroy
    void closeHttpClient() {
        httpClient.close();
    }
}
