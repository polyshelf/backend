package ru.polyshelf.gateway.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;
import ru.polyshelf.gateway.common.properties.LimitsProperties;
import ru.polyshelf.gateway.common.web.AuthRateLimitFilter;
import ru.polyshelf.gateway.common.web.RequestSizeLimitFilter;

class RequestFiltersTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void limitsAuthRequestsPerAddress() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new LimitsProperties(DataSize.ofKilobytes(1), 2), clock);

        assertThat(call(filter, "/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = call(filter, "/auth/login", "1.1.1.1");

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
        assertThat(limited.getContentAsString()).contains("RATE_LIMITED");
        assertThat(call(filter, "/auth/login", "2.2.2.2").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/products/from-barcode", "1.1.1.1").getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsOversizedBodies() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(new LimitsProperties(DataSize.ofBytes(10), 0));
        MockHttpServletRequest big = new MockHttpServletRequest("POST", "/products/from-datamatrix");
        big.setContent(new byte[11]);
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        MockHttpServletRequest small = new MockHttpServletRequest("POST", "/products/from-datamatrix");
        small.setContent(new byte[10]);
        MockHttpServletResponse accepted = new MockHttpServletResponse();

        filter.doFilter(big, rejected, new MockFilterChain());
        filter.doFilter(small, accepted, new MockFilterChain());

        assertThat(rejected.getStatus()).isEqualTo(413);
        assertThat(accepted.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletResponse call(AuthRateLimitFilter filter, String uri, String address)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
