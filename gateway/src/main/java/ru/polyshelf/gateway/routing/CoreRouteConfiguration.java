package ru.polyshelf.gateway.routing;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.DELETE;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.PUT;

import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import ru.polyshelf.gateway.common.client.InternalHeaders;
import ru.polyshelf.gateway.common.properties.ClientsProperties;
import ru.polyshelf.gateway.common.properties.InternalApiProperties;

@Configuration
public class CoreRouteConfiguration {

    private static final String PRODUCTS_PREFIX = "/products/";
    private static final String UUID_SEGMENT = "{product_uuid:[0-9a-fA-F-]+}";

    @Bean
    public RouterFunction<ServerResponse> coreRoute(ClientsProperties clients, InternalApiProperties internalApi) {
        return route("core")
                .route(inventoryOperations(), http())
                .before(uri(clients.core().url()))
                .before(internalIdentityHeaders(internalApi.apiKey()))
                .build();
    }

    static RequestPredicate inventoryOperations() {
        return POST("/products")
                .or(GET("/products/inventory"))
                .or(DELETE(PRODUCTS_PREFIX + UUID_SEGMENT))
                .or(PUT(PRODUCTS_PREFIX + UUID_SEGMENT + "/consumption"))
                .or(DELETE(PRODUCTS_PREFIX + UUID_SEGMENT + "/consumption"));
    }

    private static Function<ServerRequest, ServerRequest> internalIdentityHeaders(String apiKey) {
        return request -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return ServerRequest.from(request)
                    .headers(headers -> {
                        headers.remove(HttpHeaders.AUTHORIZATION);
                        headers.remove(InternalHeaders.USER_ID);
                        headers.set(InternalHeaders.API_KEY, apiKey);
                        if (authentication != null && authentication.isAuthenticated()) {
                            headers.set(InternalHeaders.USER_ID, authentication.getName());
                        }
                    })
                    .build();
        };
    }
}
