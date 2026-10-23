package ru.polyshelf.auth.yandex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "polyshelf.yandex")
public record YandexProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("https://login.yandex.ru/info") String infoUrl,
        @DefaultValue("5s") Duration timeout
) {
}
