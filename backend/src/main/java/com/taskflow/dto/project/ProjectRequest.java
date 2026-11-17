package com.taskflow.dto.project;

import java.time.LocalDate;

import com.taskflow.enums.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "Project name is required.")
        @Size(min = 2, max = 120, message = "Project name must be between 2 and 120 characters.")
        String name,

        @Size(max = 5000, message = "Description must be at most 5000 characters.")
        String description,

        LocalDate startDate,

        LocalDate dueDate,

        ProjectStatus status
) {
}
