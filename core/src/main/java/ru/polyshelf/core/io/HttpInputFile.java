package ru.polyshelf.core.io;

import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/** Reads a remote Parquet file using buffered HTTP ranges. Not thread-safe per stream. */
public final class HttpInputFile implements InputFile {

    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");

    private final HttpClient client;
    private final URI uri;
    private final long length;
    private final String validator;

    /** The supplied client should follow redirects for download URLs. */
    public HttpInputFile(HttpClient client, URI uri) throws IOException {
        this.client = Objects.requireNonNull(client);
        this.uri = Objects.requireNonNull(uri);
        Range first = fetch(0, 0, null);
        this.length = first.total();
        this.validator = first.validator();
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public SeekableInputStream newStream() {
        RangeInputStream stream = new RangeInputStream();
        return new DelegatingSeekableInputStream(stream) {
            @Override
            public long getPos() throws IOException {
                stream.ensureOpen();
                return stream.position;
            }

            @Override
            public void seek(long newPos) throws IOException {
                stream.ensureOpen();
                if (newPos < 0 || newPos > length) {
                    throw new EOFException("Position outside file: " + newPos);
                }
                stream.position = newPos;
            }
        };
    }

    private Range fetch(long start, long end, String ifRange) throws IOException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept-Encoding", "identity")
                .header("Range", "bytes=" + start + "-" + end);
        if (ifRange != null) {
            request.header("If-Range", ifRange);
        }

        HttpResponse<InputStream> response;
        try {
            response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading HTTP range", e);
        }

        try (InputStream body = response.body()) {
            if (response.statusCode() != 206) {
                throw new IOException("Expected HTTP 206; got " + response.statusCode()
                        + " (range unsupported or file changed)");
            }
            var range = CONTENT_RANGE.matcher(response.headers().firstValue("Content-Range").orElse(""));
            if (!range.matches() || !range.group(1).equals(Long.toString(start))
                    || !range.group(2).equals(Long.toString(end))) {
                throw new IOException("Unexpected Content-Range");
            }
            long total;
            try {
                total = Long.parseLong(range.group(3));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid file length in Content-Range", e);
            }
            if (total <= end) {
                throw new IOException("Invalid file length in Content-Range");
            }
            int expected = Math.toIntExact(end - start + 1);
            byte[] bytes = body.readNBytes(expected);
            if (bytes.length != expected) {
                throw new EOFException("Incomplete HTTP range");
            }
            String version = response.headers().firstValue("ETag")
                    .filter(etag -> !etag.startsWith("W/"))
                    .orElseGet(() -> response.headers().firstValue("Last-Modified").orElse(null));
            return new Range(total, version, bytes);
        }
    }

    private record Range(long total, String validator, byte[] bytes) {}

    private final class RangeInputStream extends InputStream {
        private long position;
        private long bufferStart;
        private byte[] buffer = new byte[0];
        private boolean closed;

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
        }

        private boolean availableInBuffer() throws IOException {
            ensureOpen();
            if (position == length) {
                return false;
            }
            if (position < bufferStart || position - bufferStart >= buffer.length) {
                long end = position + Math.min(BUFFER_SIZE, length - position) - 1;
                Range range = fetch(position, end, validator);
                if (range.total() != length) {
                    throw new IOException("Remote file length changed");
                }
                bufferStart = position;
                buffer = range.bytes();
            }
            return true;
        }

        @Override
        public int read() throws IOException {
            if (!availableInBuffer()) {
                return -1;
            }
            return buffer[(int) (position++ - bufferStart)] & 0xff;
        }

        @Override
        public int read(byte[] bytes, int offset, int count) throws IOException {
            Objects.checkFromIndexSize(offset, count, bytes.length);
            ensureOpen();
            if (count == 0) {
                return 0;
            }
            if (!availableInBuffer()) {
                return -1;
            }
            int bufferOffset = (int) (position - bufferStart);
            int size = Math.min(count, buffer.length - bufferOffset);
            System.arraycopy(buffer, bufferOffset, bytes, offset, size);
            position += size;
            return size;
        }

        @Override
        public void close() {
            closed = true;
            buffer = new byte[0];
        }
    }
}
