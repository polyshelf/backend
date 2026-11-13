package ru.polyshelf.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties("openfoodfacts")
public class OpenFoodFactsProperties {
    @Min(1)
    private int batchSize = 10000;
    @Valid
    @NotNull
    private Http http = new Http();
    @Valid
    @NotNull
    private Reader reader = new Reader();

    @AssertTrue(message = "Retry delays must be nonnegative and HTTP timeouts must be positive")
    public boolean isTimingValid() {
        return http != null && reader != null
                && nonnegative(http.retryDelay) && nonnegative(http.maxRetryDelay)
                && positive(http.requestTimeout) && positive(http.connectTimeout)
                && nonnegative(reader.retryDelay);
    }

    private static boolean nonnegative(Duration value) {
        return value != null && !value.isNegative();
    }

    private static boolean positive(Duration value) {
        return nonnegative(value) && !value.isZero();
    }

    @Data
    public static class Http {
        @Min(1)
        private int maxAttempts = 3;
        @NotNull
        private Duration retryDelay = Duration.ofSeconds(1);
        @NotNull
        private Duration maxRetryDelay = Duration.ofSeconds(30);
        @NotNull
        private Duration requestTimeout = Duration.ofSeconds(30);
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Reader {
        @Min(1)
        private int maxAttempts = 2;
        @NotNull
        private Duration retryDelay = Duration.ofSeconds(5);
    }
}
