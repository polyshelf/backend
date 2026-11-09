package ru.polyshelf.core.io;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Retries the entire response, including body reads, before exposing errors to Parquet. */
@Slf4j
public class HttpRetryExecutor {
    private static final Pattern RESET_SECONDS = Pattern.compile("(?:^|[;,\\s])t=(\\d+)");
    private final int maxAttempts;
    private final Duration retryDelay;
    private final Duration maxRetryDelay;
    private final Clock clock;
    private final Sleeper sleeper;

    public HttpRetryExecutor(int maxAttempts, Duration retryDelay, Duration maxRetryDelay) {
        this(maxAttempts, retryDelay, maxRetryDelay, Clock.systemUTC(), Thread::sleep);
    }

    HttpRetryExecutor(int maxAttempts, Duration retryDelay, Duration maxRetryDelay, Clock clock, Sleeper sleeper) {
        if (maxAttempts < 1 || retryDelay.isNegative() || maxRetryDelay.isNegative()) {
            throw new IllegalArgumentException("Invalid HTTP retry settings");
        }
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
        this.maxRetryDelay = maxRetryDelay;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public <T> T execute(HttpClient client, HttpRequest request, ResponseReader<T> reader) throws IOException {
        Duration delay = retryDelay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : retryDelay;
        for (int attempt = 1; ; attempt++) {
            Duration serverDelay = Duration.ZERO;
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    int status = response.statusCode();
                    if (status == 408 || status == 429 || status == 500 || status == 502
                            || status == 503 || status == 504) {
                        serverDelay = retryAfter(response.headers());
                        throw new IOException("Temporary HTTP status " + status);
                    }
                    if (status < 200 || status >= 300) {
                        throw new PermanentHttpException("Unexpected HTTP status " + status);
                    }
                    return reader.read(response, body);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw interrupted(e);
            } catch (IOException e) {
                if (e instanceof PermanentHttpException || Thread.currentThread().isInterrupted()
                        || attempt >= maxAttempts) {
                    throw e;
                }
                Duration wait = serverDelay.compareTo(delay) > 0 ? serverDelay : delay;
                log.warn("HTTP attempt {}/{} failed; retrying in {}: {}", attempt, maxAttempts, wait, e.getMessage());
                pause(wait);
                delay = delay.compareTo(maxRetryDelay.dividedBy(2)) >= 0 ? maxRetryDelay : delay.multipliedBy(2);
            }
        }
    }

    private Duration retryAfter(HttpHeaders headers) {
        Duration delay = Duration.ZERO;
        var value = headers.firstValue("Retry-After");
        if (value.isPresent()) {
            try {
                delay = Duration.ofSeconds(Math.max(0, Long.parseLong(value.get().strip())));
            } catch (NumberFormatException e) {
                try {
                    delay = Duration.between(clock.instant(), ZonedDateTime.parse(
                            value.get(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                } catch (DateTimeParseException ignored) {
                    // Fall back to exponential backoff or the RateLimit reset time.
                }
            }
        }
        var reset = RESET_SECONDS.matcher(headers.firstValue("RateLimit").orElse(""));
        if (reset.find()) {
            try {
                var resetDelay = Duration.ofSeconds(Long.parseLong(reset.group(1)));
                if (resetDelay.compareTo(delay) > 0) {
                    delay = resetDelay;
                }
            } catch (NumberFormatException ignored) {
                // Invalid reset values do not disable the configured backoff.
            }
        }
        return delay.isNegative() ? Duration.ZERO : delay;
    }

    private void pause(Duration delay) throws InterruptedIOException {
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw interrupted(e);
        }
    }

    private static InterruptedIOException interrupted(InterruptedException cause) {
        var failure = new InterruptedIOException("HTTP retry interrupted");
        failure.initCause(cause);
        return failure;
    }

    @FunctionalInterface
    public interface ResponseReader<T> {
        T read(HttpResponse<InputStream> response, InputStream body) throws IOException;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    public static class PermanentHttpException extends IOException {
        public PermanentHttpException(String message) {
            super(message);
        }
    }
}
