package ru.polyshelf.gateway.common.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "polyshelf.clients")
@SuppressWarnings("unused")
public record ClientsProperties(
        @Valid @NotNull ServiceProperties core,
        @Valid @NotNull ServiceProperties codeAdapter,
        @Valid @NotNull ServiceProperties catalog
) {
}
