package ru.polyshelf.it;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.polyshelf.it.Api.InventoryItem;
import ru.polyshelf.it.Api.User;
import ru.polyshelf.it.PolyshelfStack.InventoryDb;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DatamatrixInventoryFlowIT {

    private static PolyshelfStack stack;
    private static Api.Gateway api;
    private static Api.WireMock chestnyZnak;
    private static InventoryDb db;

    @BeforeAll
    static void startStack() {
        stack = new PolyshelfStack();
        stack.start();
        api = new Api.Gateway(stack.gatewayUrl());
        chestnyZnak = new Api.WireMock(stack.wiremockUrl());
        db = stack.database();
    }

    @AfterAll
    static void stopStack() {
        if (stack != null) {
            stack.close();
        }
    }

    @AfterEach
    void resetStubs() {
        chestnyZnak.reset();
    }

    static Stream<DatamatrixCase> datamatrixCases() {
        return DatamatrixCase.load().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("datamatrixCases")
    void userScansDatamatrixAndProductAppearsInHisFridge(DatamatrixCase testCase) {
        chestnyZnak.stubCheck(testCase.czCode(), testCase.czResponse());
        User user = api.registerAndLogin();
        assertThat(api.inventory(user)).as("fridge of a new user").isEmpty();

        InventoryItem scanned = api.addFromDatamatrix(user, testCase.scannedCode());

        assertThat(scanned.id()).isNotBlank();
        assertThat(scanned.name()).isEqualTo(testCase.productName());
        assertThat(scanned.expirationDate()).isEqualTo(testCase.expirationDate());
        assertFridgeHoldsOnly(user, scanned.id(), testCase, false);

        api.markConsumed(user, scanned.id());
        assertFridgeHoldsOnly(user, scanned.id(), testCase, true);

        api.restore(user, scanned.id());
        assertFridgeHoldsOnly(user, scanned.id(), testCase, false);
    }

    private void assertFridgeHoldsOnly(User user, String itemId, DatamatrixCase testCase, boolean consumed) {
        assertThat(api.inventory(user)).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(itemId);
            assertThat(item.name()).isEqualTo(testCase.productName());
            assertThat(item.expirationDate()).isEqualTo(testCase.expirationDate());
            assertThat(item.isConsumed()).isEqualTo(consumed);
        });

        InventoryDb.StoredItem stored = db.find(user, itemId);
        assertThat(stored.customName()).isEqualTo(testCase.productName());
        assertThat(stored.expirationDate()).isEqualTo(testCase.expirationDate());
        assertThat(stored.isConsumed()).isEqualTo(consumed);
        assertThat(stored.deletedAt()).isNull();
    }

    record DatamatrixCase(String scannedCode, Map<String, Object> czResponse) {

        private static final String FIXTURE = "/fixtures/datamatrix-cases.json";
        private static final char GROUP_SEPARATOR = 0x1d;

        @SuppressWarnings("unchecked")
        static List<DatamatrixCase> load() {
            List<Map<String, Object>> fixtures = Api.parse(readFixture()).read("$");
            return fixtures.stream()
                    .map(fixture -> new DatamatrixCase(
                            (String) fixture.get("datamatrix"),
                            (Map<String, Object>) fixture.get("response")))
                    .toList();
        }

        String czCode() {
            return scannedCode.replace(' ', GROUP_SEPARATOR);
        }

        String productName() {
            return (String) czResponse.get("productName");
        }

        LocalDate expirationDate() {
            long epochMillis = ((Number) czResponse.get("expireDate")).longValue();
            return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
        }

        @Override
        public @NonNull String toString() {
            return productName();
        }

        private static String readFixture() {
            try (InputStream input = DatamatrixCase.class.getResourceAsStream(FIXTURE)) {
                if (input == null) {
                    throw new IllegalStateException("Fixture not found: " + FIXTURE);
                }
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
