package com.spotnight.shared.web;

import static org.assertj.core.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void propagatesSafeIdAndCleansLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(MDC.get("requestId")).isEqualTo("request-123"));
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("request-123");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void generatesIdsForAbsentOrUnsafeHeaders() throws Exception {
        for (String value : new String[]{null, "", "bad\r\nheader", "x".repeat(65)}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            if (value != null) request.addHeader(RequestIdFilter.HEADER, value);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertThat(UUID.fromString(response.getHeader(RequestIdFilter.HEADER))).isNotNull();
            assertThat(MDC.get("requestId")).isNull();
        }
    }

    @Test
    void restoresLoggingContextOnFailure() {
        MDC.put("requestId", "outer");
        try {
            assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                    (req, res) -> { throw new ServletException("failure"); })).isInstanceOf(ServletException.class);
            assertThat(MDC.get("requestId")).isEqualTo("outer");
        } finally {
            MDC.remove("requestId");
        }
    }
}
