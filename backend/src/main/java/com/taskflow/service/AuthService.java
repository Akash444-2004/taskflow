package com.taskflow.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskflow.dto.auth.LoginRequest;
import com.taskflow.dto.auth.RegisterRequest;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.entity.User;
import com.taskflow.exception.ConflictException;
import com.taskflow.exception.UnauthorizedException;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtService;
import com.taskflow.security.TaskFlowUserPrincipal;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthTokens register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        PasswordPolicy.ensureBcryptCompatible(request.password());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists."
            );
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(normalizePhone(request.phoneNumber()));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists."
            );
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
            );
        } catch (AuthenticationException ex) {
            log.debug("Authentication failed: {}", ex.getClass().getSimpleName());
            throw invalidCredentials();
        }

        if (!(authentication.getPrincipal() instanceof TaskFlowUserPrincipal principal)) {
            throw invalidCredentials();
        }

        User user = userRepository.findById(principal.id())
                .orElseThrow(AuthService::invalidCredentials);

        if (!user.isEnabled()) {
            throw invalidCredentials();
        }

        return issueTokens(user);
    }

    public AuthTokens refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        return issueTokens(rotation.user(), rotation.refreshToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthTokens issueTokens(User user) {
        String refreshToken = refreshTokenService.issue(user);
        return issueTokens(user, refreshToken);
    }

    private AuthTokens issueTokens(User user, String refreshToken) {
        JwtService.IssuedAccessToken accessToken = jwtService.issueAccessToken(user);
        return new AuthTokens(
                accessToken.value(),
                accessToken.expiresInSeconds(),
                refreshToken,
                UserResponse.from(user)
        );
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password.");
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return null;
        }
        return phoneNumber.trim();
    }

    public record AuthTokens(
            String accessToken,
            long expiresInSeconds,
            String refreshToken,
            UserResponse user
    ) {
    }
}
