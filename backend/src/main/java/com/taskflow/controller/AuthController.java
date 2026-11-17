package com.taskflow.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.auth.AuthResponse;
import com.taskflow.dto.auth.CsrfTokenResponse;
import com.taskflow.dto.auth.LoginRequest;
import com.taskflow.dto.auth.RegisterRequest;
import com.taskflow.exception.InvalidRefreshTokenException;
import com.taskflow.security.RefreshTokenCookieService;
import com.taskflow.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(
            AuthService authService,
            RefreshTokenCookieService refreshTokenCookieService,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        noStore(response);
        AuthService.AuthTokens tokens = authService.register(request);
        refreshTokenCookieService.write(response, tokens.refreshToken());
        return AuthResponse.from(tokens.accessToken(), tokens.expiresInSeconds(), tokens.user());
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        noStore(response);
        AuthService.AuthTokens tokens = authService.login(request);
        refreshTokenCookieService.write(response, tokens.refreshToken());
        return AuthResponse.from(tokens.accessToken(), tokens.expiresInSeconds(), tokens.user());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        noStore(response);
        String refreshToken = refreshTokenCookieService.read(request).orElse(null);

        try {
            if (refreshToken == null) {
                throw new InvalidRefreshTokenException();
            }

            AuthService.AuthTokens tokens = authService.refresh(refreshToken);
            refreshTokenCookieService.write(response, tokens.refreshToken());
            return AuthResponse.from(tokens.accessToken(), tokens.expiresInSeconds(), tokens.user());
        } catch (InvalidRefreshTokenException ex) {
            refreshTokenCookieService.clear(response);
            throw ex;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        noStore(response);
        try {
            refreshTokenCookieService.read(request).ifPresent(authService::logout);
        } finally {
            refreshTokenCookieService.clear(response);
        }
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        noStore(response);

        DeferredCsrfToken deferred = (DeferredCsrfToken) request.getAttribute(
                DeferredCsrfToken.class.getName()
        );
        CsrfToken csrfToken = deferred != null
                ? deferred.get()
                : (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.loadToken(request);
        }
        if (csrfToken == null) {
            throw new IllegalStateException("CSRF token is not available.");
        }

        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    private static void noStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    }
}
