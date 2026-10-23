package ru.polyshelf.auth.yandex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YandexProfile(
        String id,
        String login,
        @JsonProperty("client_id") String clientId
) {

    public String email() {
        return login.toLowerCase(Locale.ROOT) + "@yandex.ru";
    }

}
