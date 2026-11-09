package ru.polyshelf.core.io;

import com.jerolba.carpet.CarpetReader;
import com.jerolba.carpet.CloseableIterator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.polyshelf.core.config.OpenFoodFactsProperties;
import ru.polyshelf.core.dto.OffProductDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class OpenFoodFactsSource {
    private static final String FILE_URL = "https://huggingface.co/datasets/openfoodfacts/product-database/resolve/%s/food.parquet";
    private final HttpClient httpClient;
    private final HttpRetryExecutor httpRetry;
    private final OpenFoodFactsProperties properties;

    public String latestRevision() throws IOException {
        var request = HttpRequest.newBuilder(URI.create(FILE_URL.formatted("main")))
                .timeout(properties.getHttp().getRequestTimeout())
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        return httpRetry.execute(httpClient, request, (response, body) -> {
            // Hugging Face supplies the commit on its resolver redirect, before the CDN response.
            HttpResponse<?> current = response;
            while (true) {
                var commit = current.headers().firstValue("X-Repo-Commit");
                if (commit.isPresent() && commit.get().matches("[0-9a-f]{40}")) {
                    return commit.get();
                }
                if (current.previousResponse().isEmpty()) {
                    throw new HttpRetryExecutor.PermanentHttpException("Missing dataset commit SHA");
                }
                current = current.previousResponse().orElseThrow();
            }
        });
    }

    public CloseableIterator<OffProductDto> open(String revision) throws IOException {
        if (!revision.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Expected an immutable dataset commit SHA");
        }
        var inputFile = new HttpInputFile(httpClient, URI.create(FILE_URL.formatted(revision)),
                httpRetry, properties.getHttp().getRequestTimeout());
        return new CarpetReader<>(inputFile, OffProductDto.class).iterator();
    }
}
