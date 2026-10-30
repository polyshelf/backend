package ru.polyshelf.it;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class Api {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Configuration JSON = Configuration.defaultConfiguration();

    private Api() {
    }

    record User(String email, String accessToken) {
    }

    record InventoryItem(String id, String name, LocalDate expirationDate, String consumedAt) {

        static InventoryItem from(Map<String, Object> json) {
            return new InventoryItem(
                    (String) json.get("id"),
                    (String) json.get("name"),
                    LocalDate.parse((String) json.get("expiration_date")),
                    (String) json.get("consumed_at"));
        }

        boolean isConsumed() {
            return consumedAt != null;
        }
    }

    record Reply(int status, String body) {

        Reply expectStatus(int expected) {
            assertThat(status).as("HTTP status (body: %s)", body).isEqualTo(expected);
            return this;
        }

        DocumentContext json() {
            return parse(body);
        }
    }

    static final class Gateway {

        private static final String PASSWORD = "Passw0rd-it";

        private final String baseUrl;

        Gateway(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        User registerAndLogin() {
            String email = "it-" + UUID.randomUUID() + "@polyshelf.test";
            String credentials = write(Map.of("email", email, "password", PASSWORD));

            send("POST", "/auth/register", null, credentials).expectStatus(201);
            String token = send("POST", "/auth/login", null, credentials)
                    .expectStatus(200)
                    .json().read("$.access_token", String.class);
            return new User(email, token);
        }

        List<InventoryItem> inventory(User user) {
            List<Map<String, Object>> items = send("GET", "/products/inventory", user, null)
                    .expectStatus(200)
                    .json().read("$");
            return items.stream().map(InventoryItem::from).toList();
        }

        InventoryItem addFromDatamatrix(User user, String code) {
            Map<String, Object> item = send("POST", "/products/from-datamatrix", user, write(Map.of("code", code)))
                    .expectStatus(201)
                    .json().read("$");
            return InventoryItem.from(item);
        }

        void markConsumed(User user, String itemId) {
            send("PUT", "/products/" + itemId + "/consumption", user, null).expectStatus(204);
        }

        void restore(User user, String itemId) {
            send("DELETE", "/products/" + itemId + "/consumption", user, null).expectStatus(204);
        }

        private Reply send(String method, String path, User user, String json) {
            return Api.send(method, baseUrl + path, user == null ? null : user.accessToken(), json);
        }
    }

    static final class WireMock {

        private final String baseUrl;

        WireMock(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        void stubCheck(String czCode, Map<String, Object> czResponse) {
            String expectedRequest = write(Map.of("code", czCode, "codeType", "datamatrix"));

            Map<String, Object> mapping = Map.of(
                    "request", Map.of(
                            "method", "POST",
                            "url", "/v2/mobile/check",
                            "bodyPatterns", List.of(Map.of("equalToJson", expectedRequest))),
                    "response", Map.of(
                            "status", 200,
                            "headers", Map.of("Content-Type", "application/json"),
                            "body", write(czResponse)));

            send("POST", baseUrl + "/__admin/mappings", null, write(mapping)).expectStatus(201);
        }

        void reset() {
            send("POST", baseUrl + "/__admin/reset", null, null).expectStatus(200);
        }
    }

    static String write(Object value) {
        return JSON.jsonProvider().toJson(value);
    }

    static DocumentContext parse(String json) {
        return JsonPath.using(JSON).parse(json);
    }

    private static Reply send(String method, String url, String bearerToken, String json) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url));
        if (json == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        }
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }

        try {
            HttpResponse<String> response = CLIENT.send(
                    request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Reply(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling " + method + " " + url, e);
        }
    }
}
