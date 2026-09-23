package com.comandos.core.web;

import com.comandos.core.api.IdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "platform.requestId";
    public static final String MDC_KEY = "requestId";

    private static final Pattern SAFE_ID =
        Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final IdGenerator ids;

    public RequestCorrelationFilter(IdGenerator ids) {
        this.ids = ids;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String requestId = resolve(request.getHeader(HEADER));
        long startedAt = System.nanoTime();

        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.atInfo()
                .addKeyValue("requestId", requestId)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", response.getStatus())
                .addKeyValue("durationMs", durationMs)
                .log("HTTP request completed");
            MDC.remove(MDC_KEY);
        }
    }

    private String resolve(String supplied) {
        if (supplied != null && SAFE_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return ids.next().toString();
    }
}
