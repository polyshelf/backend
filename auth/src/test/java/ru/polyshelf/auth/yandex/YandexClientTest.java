package ru.polyshelf.auth.yandex;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.polyshelf.auth.exception.AuthApiException;
import ru.polyshelf.auth.exception.AuthErrorCode;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YandexClientTest {

    private static final String CLIENT_ID = "my-client-id";

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void returnsProfileForTokenIssuedToThisApp() {
        respond(200, "{\"id\":\"1000034426\",\"login\":\"ivan\",\"client_id\":\"my-client-id\",\"psuid\":\"x\"}");

        YandexProfile profile = client(CLIENT_ID).fetchProfile("ya-token");

        assertEquals("1000034426", profile.id());
        assertEquals("ivan@yandex.ru", profile.email());
        assertEquals("OAuth ya-token", authorization.get());
    }

    @Test
    void rejectsTokenIssuedToAnotherApp() {
        respond(200, "{\"id\":\"1\",\"login\":\"ivan\",\"client_id\":\"other-app\"}");

        assertError(HttpStatus.UNAUTHORIZED, AuthErrorCode.YANDEX_TOKEN_INVALID, client(CLIENT_ID));
    }

    @Test
    void rejectsLoginWithUnexpectedCharacters() {
        respond(200, "{\"id\":\"1\",\"login\":\"a@gmail.com\",\"client_id\":\"my-client-id\"}");

        assertError(HttpStatus.UNAUTHORIZED, AuthErrorCode.YANDEX_TOKEN_INVALID, client(CLIENT_ID));
    }

    @Test
    void mapsYandexRejectionToUnauthorized() {
        respond(401, "{}");

        assertError(HttpStatus.UNAUTHORIZED, AuthErrorCode.YANDEX_TOKEN_INVALID, client(CLIENT_ID));
    }

    @Test
    void mapsYandexOutageToBadGateway() {
        respond(500, "{}");

        assertError(HttpStatus.BAD_GATEWAY, AuthErrorCode.YANDEX_UNAVAILABLE, client(CLIENT_ID));
    }

    @Test
    void failsClosedWhenClientIdIsNotConfigured() {
        assertError(HttpStatus.SERVICE_UNAVAILABLE, AuthErrorCode.YANDEX_NOT_CONFIGURED, client(""));
    }

    private void assertError(HttpStatus status, AuthErrorCode code, YandexClient client) {
        AuthApiException ex = assertThrows(AuthApiException.class, () -> client.fetchProfile("ya-token"));
        assertEquals(status, ex.getStatusCode());
        assertEquals(code, ex.getCode());
    }

    private YandexClient client(String clientId) {
        String url = "http://localhost:" + server.getAddress().getPort() + "/info";
        return new YandexClient(new YandexProperties(clientId, url, Duration.ofSeconds(2)));
    }

    private void respond(int status, String body) {
        server.createContext("/info", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

}
