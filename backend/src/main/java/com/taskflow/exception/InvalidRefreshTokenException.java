package com.taskflow.exception;

public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Your session has expired. Please sign in again.");
    }
}
