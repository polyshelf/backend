package ru.polyshelf.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.polyshelf.core.io.HttpRetryExecutor;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenFoodFactsProperties.class)
public class OpenFoodFactsConfig {
    @Bean(destroyMethod = "close")
    HttpClient openFoodFactsHttpClient(OpenFoodFactsProperties properties) {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.getHttp().getConnectTimeout())
                .build();
    }

    @Bean
    HttpRetryExecutor openFoodFactsHttpRetry(OpenFoodFactsProperties properties) {
        var http = properties.getHttp();
        return new HttpRetryExecutor(http.getMaxAttempts(), http.getRetryDelay(), http.getMaxRetryDelay());
    }
}
