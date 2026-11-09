package ru.polyshelf.core.io;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.polyshelf.core.config.OpenFoodFactsProperties;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpenFoodFactsSourceTest {
    @Test
    @SuppressWarnings("unchecked")
    void resolvesCommitFromResolverRedirectRatherThanCdnHeaders() throws Exception {
        String sha = "a".repeat(40);
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> redirect = mock(HttpResponse.class);
        when(redirect.headers()).thenReturn(HttpHeaders.of(Map.of("X-Repo-Commit", List.of(sha)), (a, b) -> true));
        HttpResponse<InputStream> cdn = mock(HttpResponse.class);
        when(cdn.statusCode()).thenReturn(200);
        when(cdn.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
        when(cdn.previousResponse()).thenReturn(Optional.of(redirect));
        when(cdn.body()).thenReturn(InputStream.nullInputStream());
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(cdn);
        var source = new OpenFoodFactsSource(client,
                new HttpRetryExecutor(1, Duration.ZERO, Duration.ZERO), new OpenFoodFactsProperties());

        assertEquals(sha, source.latestRevision());

        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("HEAD", request.getValue().method());
        assertTrue(request.getValue().uri().getPath().contains("/resolve/main/"));
    }

    @Test
    void cannotOpenMutableBranchInsteadOfPinnedCommit() {
        var source = new OpenFoodFactsSource(mock(HttpClient.class),
                new HttpRetryExecutor(1, Duration.ZERO, Duration.ZERO), new OpenFoodFactsProperties());

        assertThrows(IllegalArgumentException.class, () -> source.open("main"));
    }
}
