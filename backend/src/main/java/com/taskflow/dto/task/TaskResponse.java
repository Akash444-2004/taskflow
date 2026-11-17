package com.taskflow.dto.task;

import java.time.Instant;
import java.time.LocalDate;

import com.taskflow.dto.user.UserSummary;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        String projectName,
        UserSummary assignedTo,
        UserSummary createdBy,
        TaskPriority priority,
        TaskStatus status,
        LocalDate startDate,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}
