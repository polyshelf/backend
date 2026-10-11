package ru.polyshelf.gateway.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.polyshelf.gateway.common.error.ProblemResponses;
import ru.polyshelf.gateway.common.properties.LimitsProperties;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final HttpStatus TOO_LARGE = HttpStatus.valueOf(413);

    private final long maxBytes;

    public RequestSizeLimitFilter(LimitsProperties limits) {
        this.maxBytes = limits.maxBodySize().toBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            ProblemResponses.write(response, TOO_LARGE, "PAYLOAD_TOO_LARGE", "Request body is too large");
            return;
        }
        chain.doFilter(request, response);
    }
}
