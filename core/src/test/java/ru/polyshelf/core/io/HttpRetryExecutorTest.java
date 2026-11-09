package ru.polyshelf.core.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpRetryExecutorTest {
    private final HttpClient client = mock(HttpClient.class);
    private final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.com/file"))
            .header("Range", "bytes=100-199").build();
    private final List<Duration> delays = new ArrayList<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"), ZoneOffset.UTC);
    private final HttpRetryExecutor retry = new HttpRetryExecutor(3, Duration.ofSeconds(1),
            Duration.ofSeconds(30), clock, delays::add);

    @Test
    void rateLimitWaitsForServerResetClosesResponseAndRepeatsSameRequest() throws Exception {
        var limitedBody = spy(new ByteArrayInputStream(new byte[0]));
        var limited = response(429, Map.of("Retry-After", List.of("120"),
                "RateLimit", List.of("\"resolvers\";r=0;t=180")), limitedBody);
        var success = response(206, Map.of(), new ByteArrayInputStream(new byte[]{42}));
        when(client.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(limited, success);

        int result = retry.execute(client, request, (response, body) -> body.read());
        assertEquals(42, result);

        assertEquals(List.of(Duration.ofSeconds(180)), delays);
        verify(limitedBody).close();
        verify(client, times(2)).send(eq(request), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void acceptsRetryAfterHttpDate() throws Exception {
        var unavailable = response(503, Map.of("Retry-After", List.of("Mon, 09 Nov 2026 00:00:10 GMT")), InputStream.nullInputStream());
        var success = response(206, Map.of(), InputStream.nullInputStream());
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(unavailable, success);

        retry.execute(client, request, (response, body) -> 1);

        assertEquals(List.of(Duration.ofSeconds(10)), delays);
    }

    @Test
    void retriesWholeBodyReadAfterTruncation() throws Exception {
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenAnswer(call ->
                response(206, Map.of(), InputStream.nullInputStream()));
        int[] reads = {0};

        int result = retry.execute(client, request, (response, body) -> {
            if (reads[0]++ == 0) throw new EOFException("Truncated body");
            return 123;
        });

        assertEquals(123, result);
        assertEquals(List.of(Duration.ofSeconds(1)), delays);
        verify(client, times(2)).send(eq(request), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void nonRetryableStatusFailsImmediately() throws Exception {
        var missing = response(404, Map.of(), InputStream.nullInputStream());
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(missing);

        assertThrows(HttpRetryExecutor.PermanentHttpException.class,
                () -> retry.execute(client, request, (response, body) -> 1));

        assertTrue(delays.isEmpty());
        verify(client).send(eq(request), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void networkFailuresHaveBoundedAttemptsAndExponentialBackoff() throws Exception {
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("Offline"));

        assertThrows(IOException.class, () -> retry.execute(client, request, (response, body) -> 1));

        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), delays);
        verify(client, times(3)).send(eq(request), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void interruptionPreservesCancellationAndDoesNotRetry() throws Exception {
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        try {
            assertThrows(InterruptedIOException.class, () -> retry.execute(client, request, (response, body) -> 1));
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(delays.isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> response(int status, Map<String, List<String>> headers, InputStream body) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        when(response.body()).thenReturn(body);
        return response;
    }
}
