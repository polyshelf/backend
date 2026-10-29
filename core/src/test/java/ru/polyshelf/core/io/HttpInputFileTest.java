package ru.polyshelf.core.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpInputFileTest {

    private static final URI URI = java.net.URI.create("https://example.com/data.parquet");
    private final HttpClient client = mock(HttpClient.class);
    private final List<HttpRequest> requests = new ArrayList<>();
    private final byte[] data = new byte[2 * 1024 * 1024 + 17];

    @BeforeEach
    void serveRanges() throws Exception {
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 31);
        }
        when(client.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    requests.add(request);
                    String[] range = request.headers().firstValue("Range").orElseThrow()
                            .substring("bytes=".length()).split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    return response(206, "bytes " + start + "-" + end + "/" + data.length,
                            Arrays.copyOfRange(data, start, end + 1));
                });
    }

    @Test
    void readsAcrossBlocksAndReusesBufferedBytesAfterSeek() throws Exception {
        HttpInputFile file = new HttpInputFile(client, URI);
        assertEquals(data.length, file.getLength());

        try (var stream = file.newStream()) {
            byte[] actual = new byte[1024 * 1024 + 9];
            stream.readFully(actual);
            assertArrayEquals(Arrays.copyOf(data, actual.length), actual);
            assertEquals(actual.length, stream.getPos());
            assertEquals(3, requests.size()); // Metadata probe and two data blocks.

            stream.seek(1024 * 1024);
            assertEquals(data[1024 * 1024] & 0xff, stream.read());
            assertEquals(3, requests.size());
            assertEquals("\"version-1\"", requests.getLast().headers()
                    .firstValue("If-Range").orElseThrow());
        }
    }

    @Test
    void readsOnlyTheTailAndHandlesDirectBuffersAndEof() throws Exception {
        HttpInputFile file = new HttpInputFile(client, URI);
        try (var stream = file.newStream()) {
            stream.seek(data.length - 10);
            ByteBuffer buffer = ByteBuffer.allocateDirect(14);
            buffer.position(2).limit(12);
            stream.readFully(buffer);
            buffer.flip().position(2);
            byte[] actual = new byte[10];
            buffer.get(actual);
            assertArrayEquals(Arrays.copyOfRange(data, data.length - 10, data.length), actual);
            assertEquals("bytes=" + (data.length - 10) + "-" + (data.length - 1),
                    requests.getLast().headers().firstValue("Range").orElseThrow());
            assertEquals(-1, stream.read());
            assertEquals(0, stream.read(new byte[0]));
            assertThrows(EOFException.class, () -> stream.readFully(new byte[1]));
            assertThrows(EOFException.class, () -> stream.seek(-1));
            assertThrows(EOFException.class, () -> stream.seek(data.length + 1L));
        }
    }

    @Test
    void streamsHaveIndependentPositionsAndCannotBeUsedAfterClose() throws Exception {
        HttpInputFile file = new HttpInputFile(client, URI);
        try (var first = file.newStream(); var second = file.newStream()) {
            first.seek(100);
            assertEquals(0, second.getPos());
            first.close();
            assertThrows(IOException.class, first::read);
            assertThrows(IOException.class, () -> first.seek(0));
            assertEquals(data[0] & 0xff, second.read());
        }
    }

    @Test
    void rejectsAFullDownloadWhenRangeIsIgnored() throws Exception {
        doReturn(response(200, "", data)).when(client).send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
        assertThrows(IOException.class, () -> new HttpInputFile(client, URI));
    }

    @Test
    void rejectsTruncatedAndMismatchedRanges() throws Exception {
        HttpInputFile file = new HttpInputFile(client, URI);
        doReturn(response(206, "bytes 0-1048575/" + data.length, new byte[2]))
                .when(client).send(any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
        try (var stream = file.newStream()) {
            assertThrows(EOFException.class, stream::read);
        }

        doReturn(response(206, "bytes 1-1048576/" + data.length, new byte[0]))
                .when(client).send(any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
        try (var stream = file.newStream()) {
            assertThrows(IOException.class, stream::read);
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> response(int status, String range, byte[] body) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of(
                "Content-Range", List.of(range),
                "ETag", List.of("\"version-1\"")), (name, value) -> true));
        when(response.body()).thenReturn(new ByteArrayInputStream(body));
        return response;
    }
}
