package com.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Token-bucket rate limits applied by {@link com.taskflow.security.RateLimitFilter}.
 * All values are requests per minute, per client IP.
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("120") int globalPerMinute,
        @DefaultValue("10") int loginPerMinute,
        @DefaultValue("3") int registerPerMinute,
        @DefaultValue("5") int passwordChangePerMinute,
        @DefaultValue("true") boolean trustForwardedFor
) {
}
