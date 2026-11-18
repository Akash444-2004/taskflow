package com.taskflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.taskflow.config.RateLimitProperties;

import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void rejectsLoginAfterThePerMinuteLimitIsExhausted() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties(10), objectMapper);

        for (int attempt = 0; attempt < 10; attempt++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("203.0.113.7"), response, (request, result) -> { });
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.7"), blocked, (request, result) -> { });

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(blocked.getContentType()).contains("application/json");
        assertThat(blocked.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void tracksEachClientIpIndependently() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties(1), objectMapper);

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.1"), first, (request, result) -> { });
        assertThat(first.getStatus()).isEqualTo(200);

        MockHttpServletResponse sameClient = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.1"), sameClient, (request, result) -> { });
        assertThat(sameClient.getStatus()).isEqualTo(429);

        MockHttpServletResponse otherClient = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.2"), otherClient, (request, result) -> { });
        assertThat(otherClient.getStatus()).isEqualTo(200);
    }

    @Test
    void leavesNonApiTrafficAndPreflightRequestsAlone() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties(1), objectMapper);

        MockHttpServletResponse page = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/index.html", "198.51.100.9"), page, (req, res) -> { });
        assertThat(page.getStatus()).isEqualTo(200);

        for (int attempt = 0; attempt < 5; attempt++) {
            MockHttpServletResponse preflight = new MockHttpServletResponse();
            filter.doFilter(request("OPTIONS", "/api/auth/login", "198.51.100.9"), preflight, (req, res) -> { });
            assertThat(preflight.getStatus()).isEqualTo(200);
        }
    }

    private RateLimitProperties properties(int loginPerMinute) {
        return new RateLimitProperties(true, 120, loginPerMinute, 3, 5, true);
    }

    private MockHttpServletRequest loginRequest(String clientIp) {
        return request("POST", "/api/auth/login", clientIp);
    }

    private MockHttpServletRequest request(String method, String path, String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }
}
