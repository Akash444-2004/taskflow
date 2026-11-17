package com.taskflow.service;

import java.nio.charset.StandardCharsets;

import com.taskflow.exception.BadRequestException;

public final class PasswordPolicy {

    private static final int MAX_BCRYPT_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void ensureBcryptCompatible(String password) {
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new BadRequestException(
                    "PASSWORD_TOO_LONG",
                    "Password must be at most 72 bytes when UTF-8 encoded."
            );
        }
    }
}
