package ru.polyshelf.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoreRouteConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"/api/core/inventory", "/api/core/inventory/items/1", "/api/core/profile/internal"})
    void exposesPublicCorePaths(String path) {
        assertThat(CoreRouteConfiguration.isExposedPath(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/core/internal/auth/login", "/api/core/INTERNAL/catalog/new-product",
            "/api/core/inventory/../internal/x", "/api/core/a;b", "/api/core/%2e%2e/internal"})
    void blocksInternalAndTraversalPaths(String path) {
        assertThat(CoreRouteConfiguration.isExposedPath(path)).isFalse();
    }
}
