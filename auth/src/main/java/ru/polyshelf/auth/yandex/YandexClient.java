package ru.polyshelf.auth.yandex;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.polyshelf.auth.exception.AuthApiException;
import ru.polyshelf.auth.exception.AuthErrorCode;

import java.net.http.HttpClient;
import java.util.regex.Pattern;

@Component
public class YandexClient {

    private static final Pattern LOGIN = Pattern.compile("[A-Za-z0-9._-]+");

    private final RestClient restClient;
    private final YandexProperties properties;

    public YandexClient(YandexProperties properties) {
        this.properties = properties;
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public YandexProfile fetchProfile(String oauthToken) {
        if (properties.clientId().isBlank()) {
            throw new AuthApiException(HttpStatus.SERVICE_UNAVAILABLE, AuthErrorCode.YANDEX_NOT_CONFIGURED,
                    "Вход через Яндекс не настроен");
        }

        YandexProfile profile;
        try {
            profile = restClient.get()
                    .uri(properties.infoUrl() + "?format=json")
                    .header(HttpHeaders.AUTHORIZATION, "OAuth " + oauthToken)
                    .retrieve()
                    .body(YandexProfile.class);
        } catch (HttpClientErrorException _) {
            throw invalidToken();
        } catch (RestClientException _) {
            throw new AuthApiException(HttpStatus.BAD_GATEWAY, AuthErrorCode.YANDEX_UNAVAILABLE,
                    "Яндекс временно недоступен =(");
        }

        if (profile == null || profile.id() == null || profile.login() == null
                || !LOGIN.matcher(profile.login()).matches()
                || !properties.clientId().equals(profile.clientId())) {
            throw invalidToken();
        }
        return profile;
    }

    private static AuthApiException invalidToken() {
        return new AuthApiException(HttpStatus.UNAUTHORIZED, AuthErrorCode.YANDEX_TOKEN_INVALID,
                "Яндекс не подтвердил вход");
    }

}
