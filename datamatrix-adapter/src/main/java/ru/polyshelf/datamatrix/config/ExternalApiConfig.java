package ru.polyshelf.datamatrix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ExternalApiConfig {

    @Bean
    public RestClient externalApiRestClient(
            @Value("${external-api.base-url}") String baseUrl,
            @Value("${external-api.proxy.enabled}") boolean proxyEnabled,
            @Value("${external-api.proxy.host}") String proxyHost,
            @Value("${external-api.proxy.port}") int proxyPort
    ) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5));

        if (proxyEnabled) {
            if (proxyHost == null || proxyHost.isBlank()) {
                throw new IllegalArgumentException("Proxy host is required");
            }
            httpClientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        }

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClientBuilder.build());

        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

}