package com.taskflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
        String fullName,

        @Size(max = 30, message = "Phone number must be at most 30 characters.")
        @Pattern(
                regexp = "^\\s*$|^\\+?[0-9().\\-\\s]{7,30}$",
                message = "Phone number must contain 7 to 30 valid characters."
        )
        String phoneNumber
) {
}
