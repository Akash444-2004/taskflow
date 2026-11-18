package com.taskflow.security;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;
import com.taskflow.config.RateLimitProperties;
import com.taskflow.exception.ApiErrorResponse;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-client-IP token-bucket rate limiting.
 *
 * <p>Runs before the Spring Security chain so unauthenticated floods on
 * protected endpoints are rejected early. Limits are configured under
 * {@code app.rate-limit.*}. Buckets live in memory: this design targets the
 * single API instance used by the Render deployment. A horizontally scaled
 * deployment should move the bucket store to Redis.
 *
 * <p>When {@code trust-forwarded-for} is enabled the first {@code X-Forwarded-For}
 * hop is used, which is the real client behind the Vercel rewrite and the Render
 * proxy. Disable it only if the service is reachable without a trusted proxy.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_TRACKED_CLIENTS = 20_000;
    private static final long IDLE_EVICTION_NANOS = Duration.ofMinutes(10).toNanos();

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!properties.enabled()
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        LimitRule rule = ruleFor(path);
        String key = rule.name() + '|' + clientIp(request);
        Bucket bucket = bucketFor(key, rule);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
                1,
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1
        );

        log.warn("Rate limit '{}' exceeded for client {} at {}", rule.name(), key, path);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setHeader("X-RateLimit-Remaining", "0");

        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please retry in " + retryAfterSeconds + " seconds.",
                path
        ));
    }

    private LimitRule ruleFor(String path) {
        if ("/api/auth/login".equals(path)) {
            return new LimitRule("login", properties.loginPerMinute());
        }
        if ("/api/auth/register".equals(path)) {
            return new LimitRule("register", properties.registerPerMinute());
        }
        if ("/api/users/me/password".equals(path)) {
            return new LimitRule("password-change", properties.passwordChangePerMinute());
        }
        return new LimitRule("global", properties.globalPerMinute());
    }

    private Bucket bucketFor(String key, LimitRule rule) {
        long now = System.nanoTime();
        evictIdleClients(now);

        ClientBucket entry = buckets.computeIfAbsent(key, ignored -> new ClientBucket(
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                rule.perMinute(),
                                Refill.greedy(rule.perMinute(), Duration.ofMinutes(1))
                        ))
                        .build()
        ));
        entry.lastSeen.set(now);
        return entry.bucket;
    }

    private void evictIdleClients(long now) {
        if (buckets.size() < MAX_TRACKED_CLIENTS) {
            return;
        }
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastSeen.get() > IDLE_EVICTION_NANOS);
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                int comma = forwardedFor.indexOf(',');
                return (comma > 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
            }
        }
        return request.getRemoteAddr();
    }

    private record LimitRule(String name, int perMinute) {
    }

    private static final class ClientBucket {

        private final Bucket bucket;
        private final AtomicLong lastSeen = new AtomicLong(System.nanoTime());

        private ClientBucket(Bucket bucket) {
            this.bucket = bucket;
        }
    }
}
