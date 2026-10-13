package ru.polyshelf.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;

class CoreRouteConfigurationTest {

    private static final String ID = "acde070d-8c4c-4f0d-9d8a-162843c10333";

    @ParameterizedTest
    @CsvSource({
            "POST,/products",
            "GET,/products/inventory",
            "DELETE,/products/" + ID,
            "PUT,/products/" + ID + "/consumption",
            "DELETE,/products/" + ID + "/consumption"
    })
    void proxiesInventoryOperations(String method, String path) {
        assertThat(matches(method, path)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/products",
            "POST,/products/from-barcode",
            "POST,/products/inventory",
            "DELETE,/products/inventory",
            "GET,/products/" + ID,
            "POST,/products/" + ID + "/consumption",
            "DELETE,/products/..",
            "DELETE,/products/not-a-uuid",
            "GET,/internal/auth/login",
            "GET,/api/core/inventory"
    })
    void doesNotProxyAnythingElse(String method, String path) {
        assertThat(matches(method, path)).isFalse();
    }

    private static boolean matches(String method, String path) {
        ServerRequest request = ServerRequest.create(new MockHttpServletRequest(method, path), List.of());
        return CoreRouteConfiguration.inventoryOperations().test(request);
    }
}
