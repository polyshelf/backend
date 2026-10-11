package ru.polyshelf.gateway.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.polyshelf.gateway.common.error.ProblemResponses;
import ru.polyshelf.gateway.common.properties.LimitsProperties;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int CLEANUP_THRESHOLD = 10_000;
    private static final String AUTH_PREFIX = "/api/auth/";

    private final int limit;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(LimitsProperties limits, Clock clock) {
        this.limit = limits.authRequestsPerMinute();
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limit <= 0 || !request.getRequestURI().startsWith(AUTH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.millis();
        Window window = windows.compute(request.getRemoteAddr(), (key, old) ->
                old == null || now - old.startedAt() >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(old.startedAt(), old.count() + 1));
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(value -> now - value.startedAt() >= WINDOW_MILLIS);
        }
        if (window.count() > limit) {
            long retryAfter = Math.max(1, (window.startedAt() + WINDOW_MILLIS - now + 999) / 1000);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            ProblemResponses.write(response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many requests");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Window(long startedAt, int count) {
    }
}
