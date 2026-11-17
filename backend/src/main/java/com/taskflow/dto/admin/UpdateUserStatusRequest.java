package com.taskflow.dto.admin;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Enabled flag is required.")
        Boolean enabled
) {
}
