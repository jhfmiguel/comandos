package com.comandos.core.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fariamiguel.core.api.IdGenerator;
import com.fariamiguel.core.web.RequestCorrelationFilter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTests {

    private static final UUID GENERATED = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String HEADER = "X-Request-Id";
    private final IdGenerator ids = () -> GENERATED;

    @Test
    void preservesIncomingRequestId() throws Exception {
        var filter = new RequestCorrelationFilter(ids, HEADER);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(HEADER, "client-req-42");

        filter.doFilter(request, response, (req, res) ->
            assertEquals("client-req-42", req.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE))
        );

        assertEquals("client-req-42", response.getHeader(HEADER));
    }

    @Test
    void generatesRequestIdWhenIncomingValueIsMissingOrBlank() throws Exception {
        var filter = new RequestCorrelationFilter(ids, HEADER);
        for (String supplied : new String[] { null, "", "   " }) {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();
            if (supplied != null) request.addHeader(HEADER, supplied);

            filter.doFilter(request, response, (req, res) ->
                assertEquals(GENERATED.toString(), req.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE))
            );

            assertEquals(GENERATED.toString(), response.getHeader(HEADER));
        }
    }

    @Test
    void trimsIncomingRequestIdAndUsesConfiguredHeader() throws Exception {
        var filter = new RequestCorrelationFilter(ids, "X-Correlation-Id");
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader("X-Correlation-Id", "  external-123  ");

        filter.doFilter(request, response, (req, res) ->
            assertEquals("external-123", req.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE))
        );

        assertEquals("external-123", response.getHeader("X-Correlation-Id"));
        assertNull(response.getHeader(HEADER));
    }

    @Test
    void preservesCorrelationResponseHeaderWhenRequestFails() {
        var filter = new RequestCorrelationFilter(ids, HEADER);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        var failure = assertThrows(IllegalStateException.class, () -> filter.doFilter(
            request, response, (req, res) -> {
                assertEquals(GENERATED.toString(), req.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE));
                throw new IllegalStateException("boom");
            }));

        assertEquals("boom", failure.getMessage());
        assertEquals(GENERATED.toString(), response.getHeader(HEADER));
    }
}
