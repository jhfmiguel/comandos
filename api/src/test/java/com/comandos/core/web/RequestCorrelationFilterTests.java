package com.comandos.core.web;

import static org.junit.jupiter.api.Assertions.*;

import com.comandos.core.api.IdGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTests {

    private static final UUID GENERATED =
        UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final IdGenerator ids = () -> GENERATED;

    @Test
    void preservesSafeIncomingRequestId() throws Exception {
        var filter = new RequestCorrelationFilter(ids);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        request.addHeader(RequestCorrelationFilter.HEADER, "client-req-42");

        filter.doFilter(
            request,
            response,
            (req, res) -> {
                assertEquals(
                    "client-req-42",
                    MDC.get(RequestCorrelationFilter.MDC_KEY)
                );
                assertEquals(
                    "client-req-42",
                    req.getAttribute(RequestCorrelationFilter.ATTRIBUTE)
                );
            }
        );

        assertEquals(
            "client-req-42",
            response.getHeader(RequestCorrelationFilter.HEADER)
        );
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void generatesRequestIdWhenIncomingValueIsMissingOrUnsafe() throws Exception {
        var filter = new RequestCorrelationFilter(ids);

        for (String supplied : new String[] { null, "invalid request id", "<script>" }) {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();

            if (supplied != null) {
                request.addHeader(RequestCorrelationFilter.HEADER, supplied);
            }

            filter.doFilter(
                request,
                response,
                (req, res) -> assertEquals(
                    GENERATED.toString(),
                    MDC.get(RequestCorrelationFilter.MDC_KEY)
                )
            );

            assertEquals(
                GENERATED.toString(),
                response.getHeader(RequestCorrelationFilter.HEADER)
            );
            assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
        }
    }
}
