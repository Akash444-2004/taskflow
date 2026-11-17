package com.taskflow.dto.task;

import java.time.LocalDate;

import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank(message = "Task title is required.")
        @Size(min = 2, max = 160, message = "Task title must be between 2 and 160 characters.")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters.")
        String description,

        Long assignedToId,

        TaskPriority priority,

        TaskStatus status,

        LocalDate startDate,

        LocalDate dueDate
) {
}
