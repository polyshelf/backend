package ru.polyshelf.gateway.routing;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import ru.polyshelf.gateway.common.client.InternalHeaders;
import ru.polyshelf.gateway.common.properties.ClientsProperties;
import ru.polyshelf.gateway.common.properties.InternalApiProperties;

@Configuration
public class CoreRouteConfiguration {

    private static final String CORE_PATH_PATTERN = "/api/core/**";
    private static final int CORE_PATH_SEGMENTS = 2;

    @Bean
    public RouterFunction<ServerResponse> coreRoute(ClientsProperties clients, InternalApiProperties internalApi) {
        return route("core")
                .route(path(CORE_PATH_PATTERN).and(request -> isExposedPath(request.path())), http())
                .before(uri(clients.core().url()))
                .before(stripPrefix(CORE_PATH_SEGMENTS))
                .before(internalIdentityHeaders(internalApi.apiKey()))
                .build();
    }

    /**
     * Служебный API ядра (/internal/**) не должен быть достижим через прокси: gateway сам добавляет
     * внутренний ключ, и клиент мог бы вызвать его с чужим userId.
     */
    static boolean isExposedPath(String path) {
        List<String> segments = Arrays.stream(path.split("/")).filter(segment -> !segment.isEmpty()).toList();
        List<String> rest = segments.size() > CORE_PATH_SEGMENTS
                ? segments.subList(CORE_PATH_SEGMENTS, segments.size())
                : List.of();
        boolean suspicious = rest.stream().anyMatch(segment ->
                segment.equals("..") || segment.equals(".") || segment.contains(";") || segment.contains("%"));
        return !suspicious && (rest.isEmpty() || !rest.get(0).equalsIgnoreCase("internal"));
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
