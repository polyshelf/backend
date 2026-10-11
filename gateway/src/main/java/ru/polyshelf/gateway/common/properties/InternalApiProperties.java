package ru.polyshelf.gateway.common.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "polyshelf.internal")
public record InternalApiProperties(@NotBlank String apiKey) {
}
