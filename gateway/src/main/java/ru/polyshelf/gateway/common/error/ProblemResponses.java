package ru.polyshelf.gateway.common.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;

public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase()
                + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detail
                + "\",\"code\":\"" + code + "\"}");
    }
}
