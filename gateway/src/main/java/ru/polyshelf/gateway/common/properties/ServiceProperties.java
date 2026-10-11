package ru.polyshelf.gateway.common.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.bind.DefaultValue;

public record ServiceProperties(
        @NotBlank String url,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout
) {
}
