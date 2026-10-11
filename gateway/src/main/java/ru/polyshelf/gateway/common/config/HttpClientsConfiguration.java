package ru.polyshelf.gateway.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.polyshelf.gateway.common.client.InternalHeaders;
import ru.polyshelf.gateway.common.client.RestClientFactory;
import ru.polyshelf.gateway.common.properties.ClientsProperties;
import ru.polyshelf.gateway.common.properties.InternalApiProperties;

@Configuration
public class HttpClientsConfiguration {

    public static final String CORE_CLIENT = "coreRestClient";
    public static final String CODE_ADAPTER_CLIENT = "codeAdapterRestClient";
    public static final String CATALOG_CLIENT = "catalogRestClient";

    @Bean(CORE_CLIENT)
    public RestClient coreRestClient(ClientsProperties clients, InternalApiProperties internalApi) {
        return RestClientFactory.builder(clients.core())
                .defaultHeader(InternalHeaders.API_KEY, internalApi.apiKey())
                .build();
    }

    @Bean(CATALOG_CLIENT)
    public RestClient catalogRestClient(ClientsProperties clients, InternalApiProperties internalApi) {
        return RestClientFactory.builder(clients.catalog())
                .defaultHeader(InternalHeaders.API_KEY, internalApi.apiKey())
                .build();
    }

    @Bean(CODE_ADAPTER_CLIENT)
    public RestClient codeAdapterRestClient(ClientsProperties clients) {
        return RestClientFactory.builder(clients.codeAdapter()).build();
    }
}
