package com.taskflow.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskflow.config.JwtProperties;
import com.taskflow.entity.RefreshToken;
import com.taskflow.entity.User;
import com.taskflow.exception.InvalidRefreshTokenException;
import com.taskflow.repository.RefreshTokenRepository;

import org.hibernate.Hibernate;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotationResult rotate(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            // A revoked token was replayed. Assume the session may be compromised
            // and invalidate every active refresh token for this account.
            refreshTokenRepository.revokeAllActiveByUserId(current.getUser().getId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (!current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            throw new InvalidRefreshTokenException();
        }

        User user = current.getUser();
        Hibernate.initialize(user);

        if (!user.isEnabled()) {
            current.setRevokedAt(now);
            refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
            throw new InvalidRefreshTokenException();
        }

        current.setRevokedAt(now);
        String nextRefreshToken = issue(user);

        return new RotationResult(user, nextRefreshToken);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }

        refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(Instant.now());
                    }
                });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    public record RotationResult(User user, String refreshToken) {
    }
}
