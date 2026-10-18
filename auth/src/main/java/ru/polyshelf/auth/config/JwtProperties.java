package ru.polyshelf.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "polyshelf.jwt")
public record JwtProperties(
        Resource privateKeyLocation,
        Duration accessTtl,
        Duration refreshTtl
) {

}
