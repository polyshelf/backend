package ru.polyshelf.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final HttpServer CORE = startStub("core");
    private static final HttpServer AUTH = startStub("auth");

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void serviceUrls(DynamicPropertyRegistry registry) {
        registry.add("POLYSHELF_CORE_URL", () -> "http://127.0.0.1:" + CORE.getAddress().getPort());
        registry.add("POLYSHELF_AUTH_URL", () -> "http://127.0.0.1:" + AUTH.getAddress().getPort());
    }

    @AfterAll
    static void stopStubs() {
        CORE.stop(0);
        AUTH.stop(0);
    }

    @Test
    void forwardsProductsToCoreAsIsAndKeepsAuthorization() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/products/inventory?page=1"))
                .header("Authorization", "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("core|GET|/products/inventory?page=1|Bearer test-token|", response.body());
    }

    @Test
    void forwardsAuthToAuthServiceAndKeepsBody() throws Exception {
        String json = "{\"email\":\"user@example.com\",\"password\":\"secret1\"}";
        HttpRequest request = HttpRequest.newBuilder(uri("/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("auth|POST|/auth/login||" + json, response.body());
    }

    @Test
    void forwardsAnyOtherPathToCore() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/anything/foo")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("core|GET|/anything/foo||", response.body());
    }

    private URI uri(String pathAndQuery) {
        return URI.create("http://127.0.0.1:" + port + pathAndQuery);
    }

    private static HttpServer startStub(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            server.createContext("/", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                String answer = String.join("|",
                        name,
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().toString(),
                        auth == null ? "" : auth,
                        body);
                byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
