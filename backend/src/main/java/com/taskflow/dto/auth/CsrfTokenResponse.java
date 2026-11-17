package com.taskflow.dto.auth;

public record CsrfTokenResponse(
        String headerName,
        String token
) {
}
