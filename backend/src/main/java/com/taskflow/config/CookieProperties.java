package com.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookies")
public record CookieProperties(
        boolean secure,
        String refreshName,
        String refreshPath
) {
}
