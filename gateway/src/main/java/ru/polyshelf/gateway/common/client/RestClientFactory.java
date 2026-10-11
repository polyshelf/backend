package ru.polyshelf.gateway.common.client;

import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.polyshelf.gateway.common.properties.ServiceProperties;

public final class RestClientFactory {

    private RestClientFactory() {
    }

    public static RestClient.Builder builder(ServiceProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory);
    }
}
