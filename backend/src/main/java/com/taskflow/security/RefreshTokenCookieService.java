package com.taskflow.security;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.taskflow.config.CookieProperties;
import com.taskflow.config.JwtProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RefreshTokenCookieService {

    private static final String SAME_SITE = "Strict";

    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public RefreshTokenCookieService(CookieProperties cookieProperties, JwtProperties jwtProperties) {
        if (!StringUtils.hasText(cookieProperties.refreshName())) {
            throw new IllegalStateException("app.cookies.refresh-name must be configured.");
        }
        if (!StringUtils.hasText(cookieProperties.refreshPath())) {
            throw new IllegalStateException("app.cookies.refresh-path must be configured.");
        }
        this.cookieProperties = cookieProperties;
        this.jwtProperties = jwtProperties;
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (cookieProperties.refreshName().equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public void write(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = baseCookie(refreshToken)
                .maxAge(jwtProperties.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieProperties.refreshName(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(cookieProperties.refreshPath());
    }
}
