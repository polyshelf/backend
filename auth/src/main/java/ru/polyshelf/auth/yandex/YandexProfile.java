package ru.polyshelf.auth.yandex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YandexProfile(
        String id,
        String login,
        @JsonProperty("client_id") String clientId
) {

    private static final List<String> DOMAINS = List.of("yandex.ru", "ya.ru", "yandex.com", "yandex.by", "yandex.kz");

    public String email() {
        return login.toLowerCase(Locale.ROOT) + "@yandex.ru";
    }

    public List<String> emails() {
        String name = login.toLowerCase(Locale.ROOT);
        return DOMAINS.stream().map(domain -> name + "@" + domain).toList();
    }

}
